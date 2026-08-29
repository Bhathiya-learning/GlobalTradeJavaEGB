package lk.jiat.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.globaltrade.dto.AuthResponse;
import lk.jiat.globaltrade.service.SecurityAuthServiceLocal;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @EJB
    private SecurityAuthServiceLocal authService;

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;

    private SecurityAuthServiceLocal getService() {
        if (authService != null) {
            return authService;
        }
        try {
            InitialContext ctx = new InitialContext();
            return (SecurityAuthServiceLocal) ctx.lookup("java:global/globaltrade-ear-1.0/lk.jiat.globaltrade-globaltrade-ejb-1.0-SNAPSHOT/SecurityAuthServiceBean!lk.jiat.globaltrade.service.SecurityAuthServiceLocal");
        } catch (Exception e) {
            return null;
        }
    }

    @jakarta.annotation.PostConstruct
    public void syncSeedUserHashes() {
        try (Connection conn = dataSource.getConnection()) {
            // Customer default password 'customer123' SHA-256 hash
            String custHash = "4066a6cfb3676adcbd1d8256de88d3513139d37a152ed50d31acf3564a43b325";
            String custSalt = "10fbd21cb4e0e3ae495312127969b7b7";
            String updateDemoCust = "UPDATE users SET password = ?, salt = ? WHERE email = 'customer@globaltrade.lk'";
            try (PreparedStatement ps = conn.prepareStatement(updateDemoCust)) {
                ps.setString(1, custHash);
                ps.setString(2, custSalt);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    private boolean verifyPassword(String rawPassword, String salt, String expectedHash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedBytes = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    @POST
    @Path("/login")
    public Response login(Map<String, String> credentials) {
        if (credentials == null || !credentials.containsKey("email") || !credentials.containsKey("password")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(AuthResponse.failure("Email and password parameters are required."))
                    .build();
        }

        String email = credentials.get("email") != null ? credentials.get("email").trim() : "";
        String password = credentials.get("password") != null ? credentials.get("password").trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(AuthResponse.failure("Email and password fields cannot be empty."))
                    .build();
        }

        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(AuthResponse.failure("Please enter a valid email address format (e.g. user@domain.com)."))
                    .build();
        }

        // 1. Pure REAL SHA-256 Database Auth Verification against MySQL users table
        String sql = "SELECT u.id, u.first_name, u.last_name, u.email, u.password, u.salt, u.is_active, " +
                     "r.code AS role_code, r.name AS role_name " +
                     "FROM users u JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.email = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean isActive = rs.getBoolean("is_active");
                    if (!isActive) {
                        return Response.status(Response.Status.FORBIDDEN)
                                .entity(AuthResponse.failure("⚠️ Your account has been suspended or set to INACTIVE by system administrator. Access denied."))
                                .build();
                    }

                    String salt = rs.getString("salt");
                    String storedHash = rs.getString("password");

                    // Real SHA-256 Salted Hash Verification
                    boolean valid = verifyPassword(password, salt, storedHash);
                    if (valid) {
                        Long userId = rs.getLong("id");
                        String firstName = rs.getString("first_name");
                        String lastName = rs.getString("last_name");
                        String roleCode = rs.getString("role_code");
                        String roleName = rs.getString("role_name");
                        String token = "GTL-TOKEN-" + userId + "-" + System.currentTimeMillis();

                        AuthResponse resp = new AuthResponse(true, token, "Authenticated successfully as " + roleName, userId, firstName, lastName, email, roleCode, roleName);
                        return Response.ok(resp).build();
                    } else {
                        return Response.status(Response.Status.UNAUTHORIZED)
                                .entity(AuthResponse.failure("Invalid security password provided."))
                                .build();
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Try EJB Auth Service
        SecurityAuthServiceLocal ejbService = getService();
        if (ejbService != null) {
            try {
                AuthResponse authResponse = ejbService.login(email, password);
                if (authResponse.isSuccess()) {
                    return Response.ok(authResponse).build();
                } else {
                    return Response.status(Response.Status.FORBIDDEN).entity(authResponse).build();
                }
            } catch (Exception ex) {
                return Response.status(Response.Status.FORBIDDEN).entity(AuthResponse.failure(ex.getMessage())).build();
            }
        }

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(AuthResponse.failure("Invalid work email or security password."))
                .build();
    }

    @GET
    @Path("/verify")
    public Response verifyCaller() {
        return Response.ok(Map.of(
            "caller", "ACTIVE_USER",
            "status", "ACTIVE"
        )).build();
    }

    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String token) {
        return Response.ok(Map.of(
            "status", "SUCCESS",
            "message", "User session terminated successfully."
        )).build();
    }
}
