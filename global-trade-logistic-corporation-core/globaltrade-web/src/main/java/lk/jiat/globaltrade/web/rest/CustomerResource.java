package lk.jiat.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;

    private String hashPassword(String rawPassword, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedBytes = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return rawPassword;
        }
    }

    @GET
    public Response getAllCustomers() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT c.id AS customer_id, c.customer_code, c.company_name, c.eori_number, c.tax_id, " +
                     "c.shipping_address, c.credit_limit, c.created_at, " +
                     "u.id AS user_id, u.first_name, u.last_name, u.email, u.mobile, u.is_active " +
                     "FROM users u " +
                     "LEFT JOIN customers c ON u.id = c.user_id " +
                     "LEFT JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.role_id = 6 OR r.name = 'CUSTOMER' OR c.id IS NOT NULL " +
                     "ORDER BY u.id DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                Long customerId = rs.getObject("customer_id") != null ? rs.getLong("customer_id") : rs.getLong("user_id");
                map.put("id", customerId);
                map.put("userId", rs.getLong("user_id"));
                map.put("customerCode", rs.getString("customer_code") != null ? rs.getString("customer_code") : "CUST-US-900" + rs.getLong("user_id"));
                map.put("firstName", rs.getString("first_name"));
                map.put("lastName", rs.getString("last_name"));
                map.put("fullName", rs.getString("first_name") + " " + rs.getString("last_name"));
                map.put("email", rs.getString("email"));
                map.put("mobile", rs.getString("mobile"));
                map.put("companyName", rs.getString("company_name"));
                map.put("eoriNumber", rs.getString("eori_number"));
                map.put("shippingAddress", rs.getString("shipping_address"));
                map.put("creditLimit", rs.getBigDecimal("credit_limit") != null ? rs.getBigDecimal("credit_limit") : new BigDecimal("50000.00"));
                map.put("isActive", rs.getBoolean("is_active"));
                list.add(map);
            }
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/profile")
    public Response getCustomerProfile(@QueryParam("userId") Long userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "userId parameter is required"))
                    .build();
        }

        String sql = "SELECT u.id AS user_id, u.first_name, u.last_name, u.email, u.mobile, " +
                     "c.id AS customer_id, c.customer_code, c.company_name, c.eori_number, c.tax_id, " +
                     "c.shipping_address, c.billing_address, c.credit_limit, c.avatar_url " +
                     "FROM users u " +
                     "LEFT JOIN customers c ON u.id = c.user_id " +
                     "WHERE u.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", rs.getLong("user_id"));
                    map.put("firstName", rs.getString("first_name"));
                    map.put("lastName", rs.getString("last_name"));
                    map.put("email", rs.getString("email"));
                    map.put("mobile", rs.getString("mobile"));
                    map.put("customerId", rs.getObject("customer_id"));
                    map.put("customerCode", rs.getString("customer_code"));
                    map.put("companyName", rs.getString("company_name"));
                    map.put("eoriNumber", rs.getString("eori_number"));
                    map.put("taxId", rs.getString("tax_id"));
                    map.put("shippingAddress", rs.getString("shipping_address"));
                    map.put("billingAddress", rs.getString("billing_address"));
                    map.put("creditLimit", rs.getBigDecimal("credit_limit"));
                    map.put("avatarUrl", rs.getString("avatar_url"));
                    return Response.ok(map).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Customer profile not found for user ID: " + userId))
                            .build();
                }
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/create")
    public Response createCustomer(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("firstName") || !payload.containsKey("email")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "firstName and email are required"))
                    .build();
        }

        String firstName = payload.get("firstName").toString();
        String lastName = payload.containsKey("lastName") ? payload.get("lastName").toString() : "";
        String email = payload.get("email").toString();
        String password = payload.containsKey("password") ? payload.get("password").toString() : "customer123";
        String mobile = payload.containsKey("mobile") ? payload.get("mobile").toString() : "";
        String companyName = payload.containsKey("companyName") ? payload.get("companyName").toString() : "";
        String eoriNumber = payload.containsKey("eoriNumber") ? payload.get("eoriNumber").toString() : "";
        String shippingAddress = payload.containsKey("shippingAddress") ? payload.get("shippingAddress").toString() : "";
        BigDecimal creditLimit = payload.containsKey("creditLimit") ? new BigDecimal(payload.get("creditLimit").toString()) : new BigDecimal("50000.00");

        String salt = "10fbd21cb4e0e3ae495312127969b7b7";
        String hash = hashPassword(password, salt);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Create User
                long userId = 0;
                String userSql = "INSERT INTO users (first_name, last_name, email, password, salt, mobile, gender_id, role_id, is_active) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, 1, 6, TRUE)";
                try (PreparedStatement psUser = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    psUser.setString(1, firstName);
                    psUser.setString(2, lastName);
                    psUser.setString(3, email);
                    psUser.setString(4, hash);
                    psUser.setString(5, salt);
                    psUser.setString(6, mobile);
                    psUser.executeUpdate();
                    try (ResultSet rs = psUser.getGeneratedKeys()) {
                        if (rs.next()) userId = rs.getLong(1);
                    }
                }

                if (userId == 0) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "Failed to generate user ID"))
                            .build();
                }

                String custCode = payload.containsKey("customerCode") && payload.get("customerCode") != null ?
                        payload.get("customerCode").toString() : "CUST-US-" + (8000 + userId);

                // 2. Create Customer Profile Record
                String custSql = "INSERT INTO customers (customer_code, user_id, company_name, eori_number, shipping_address, credit_limit) " +
                                 "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psCust = conn.prepareStatement(custSql)) {
                    psCust.setString(1, custCode);
                    psCust.setLong(2, userId);
                    psCust.setString(3, companyName);
                    psCust.setString(4, eoriNumber);
                    psCust.setString(5, shippingAddress);
                    psCust.setBigDecimal(6, creditLimit);
                    psCust.executeUpdate();
                }

                conn.commit();
                return Response.ok(Map.of("success", true, "message", "New Shipper Customer registered successfully.", "userId", userId)).build();
            } catch (Exception ex) {
                conn.rollback();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", ex.getMessage()))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/update")
    public Response updateCustomer(Map<String, Object> payload) {
        return updateCustomerProfile(payload);
    }

    @POST
    @Path("/profile/update")
    public Response updateCustomerProfile(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("userId")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "userId is required"))
                    .build();
        }

        Long userId = Long.valueOf(payload.get("userId").toString());
        String firstName = payload.containsKey("firstName") ? payload.get("firstName").toString() : null;
        String lastName = payload.containsKey("lastName") ? payload.get("lastName").toString() : null;
        String mobile = payload.containsKey("mobile") ? payload.get("mobile").toString() : null;
        String companyName = payload.containsKey("companyName") ? payload.get("companyName").toString() : null;
        String eoriNumber = payload.containsKey("eoriNumber") ? payload.get("eoriNumber").toString() : null;
        String shippingAddress = payload.containsKey("shippingAddress") ? payload.get("shippingAddress").toString() : null;
        String avatarUrl = payload.containsKey("avatarUrl") ? payload.get("avatarUrl").toString() : null;
        BigDecimal creditLimit = payload.containsKey("creditLimit") ? new BigDecimal(payload.get("creditLimit").toString()) : null;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Update users table
                if (firstName != null && lastName != null) {
                    String userSql = "UPDATE users SET first_name = ?, last_name = ?" + (mobile != null ? ", mobile = ?" : "") + " WHERE id = ?";
                    try (PreparedStatement psUser = conn.prepareStatement(userSql)) {
                        psUser.setString(1, firstName);
                        psUser.setString(2, lastName);
                        if (mobile != null) {
                            psUser.setString(3, mobile);
                            psUser.setLong(4, userId);
                        } else {
                            psUser.setLong(3, userId);
                        }
                        psUser.executeUpdate();
                    }
                }

                // 2. Upsert customers table
                String custSql = "INSERT INTO customers (customer_code, user_id, company_name, eori_number, shipping_address, credit_limit, avatar_url) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE company_name = COALESCE(VALUES(company_name), company_name), " +
                                 "eori_number = COALESCE(VALUES(eori_number), eori_number), " +
                                 "shipping_address = COALESCE(VALUES(shipping_address), shipping_address), " +
                                 "credit_limit = COALESCE(VALUES(credit_limit), credit_limit), " +
                                 "avatar_url = COALESCE(VALUES(avatar_url), avatar_url)";
                try (PreparedStatement psCust = conn.prepareStatement(custSql)) {
                    psCust.setString(1, "CUST-US-" + userId);
                    psCust.setLong(2, userId);
                    psCust.setString(3, companyName);
                    psCust.setString(4, eoriNumber);
                    psCust.setString(5, shippingAddress);
                    psCust.setBigDecimal(6, creditLimit != null ? creditLimit : new BigDecimal("50000.00"));
                    psCust.setString(7, avatarUrl);
                    psCust.executeUpdate();
                }

                conn.commit();
                return Response.ok(Map.of("success", true, "message", "Customer profile saved successfully.")).build();
            } catch (Exception ex) {
                conn.rollback();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", ex.getMessage()))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") Long id) {
        String sql1 = "DELETE FROM customers WHERE id = ? OR user_id = ?";
        String sql2 = "DELETE FROM users WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement(sql1)) {
                    ps1.setLong(1, id);
                    ps1.setLong(2, id);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                    ps2.setLong(1, id);
                    ps2.executeUpdate();
                }
                conn.commit();
                return Response.ok(Map.of("success", true, "message", "Customer account deleted successfully.")).build();
            } catch (Exception ex) {
                conn.rollback();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", ex.getMessage()))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/toggle-status/{id}")
    public Response toggleCustomerStatus(@PathParam("id") Long id) {
        String sql = "UPDATE users SET is_active = NOT is_active WHERE id = (SELECT user_id FROM customers WHERE id = ?) OR id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, id);
            ps.executeUpdate();

            String checkSql = "SELECT is_active FROM users WHERE id = (SELECT user_id FROM customers WHERE id = ?) OR id = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setLong(1, id);
                checkPs.setLong(2, id);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        boolean newStatus = rs.getBoolean("is_active");
                        return Response.ok(Map.of("status", "SUCCESS", "id", id, "isActive", newStatus)).build();
                    }
                }
            }
            return Response.ok(Map.of("status", "SUCCESS", "id", id, "isActive", true)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
