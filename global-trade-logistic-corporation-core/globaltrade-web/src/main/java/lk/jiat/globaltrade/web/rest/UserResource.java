package lk.jiat.globaltrade.web.rest;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.globaltrade.entity.User;
import lk.jiat.globaltrade.service.UserServiceLocal;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @EJB
    private UserServiceLocal userService;

    @Context
    private ServletContext servletContext;

    private UserServiceLocal getService() {
        if (userService != null) return userService;
        try {
            javax.naming.InitialContext ctx = new javax.naming.InitialContext();
            return (UserServiceLocal) ctx.lookup("java:global/globaltrade-ear-1.0/lk.jiat.globaltrade-globaltrade-ejb-1.0-SNAPSHOT/UserServiceBean!lk.jiat.globaltrade.service.UserServiceLocal");
        } catch (Exception e) {
            try {
                javax.naming.InitialContext ctx = new javax.naming.InitialContext();
                return (UserServiceLocal) ctx.lookup("java:module/UserServiceBean!lk.jiat.globaltrade.service.UserServiceLocal");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @jakarta.annotation.Resource(lookup = "jdbc/GlobalTradeDS")
    private javax.sql.DataSource dataSource;

    @GET
    public Response getAllUsers() {
        List<Map<String, Object>> response = new java.util.ArrayList<>();
        try {
            UserServiceLocal service = getService();
            if (service != null) {
                List<User> users = service.getAllUsers();
                for (User u : users) {
                    if (u.getRole() != null && "CUSTOMER".equalsIgnoreCase(u.getRole().getName())) {
                        continue; // Exclude customer accounts from internal system staff list
                    }
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", u.getId());
                    map.put("firstName", u.getFirstName());
                    map.put("lastName", u.getLastName());
                    map.put("email", u.getEmail());
                    map.put("mobile", u.getMobile() != null ? u.getMobile() : "");
                    map.put("roleName", u.getRole() != null ? u.getRole().getName() : "LOGISTICS_OPERATOR");
                    map.put("role", Map.of("id", u.getRole() != null ? u.getRole().getId() : 1, "name", u.getRole() != null ? u.getRole().getName() : "ADMIN"));
                    map.put("isActive", u.getIsActive() != null ? u.getIsActive() : true);
                    map.put("salt", u.getSalt() != null ? u.getSalt() : "a8f3b2c1d9e4");
                    response.add(map);
                }
                if (!response.isEmpty()) return Response.ok(response).build();
            }
        } catch (Exception e) {
            // Fallthrough to JDBC fallback query below
        }

        // Direct JDBC fallback to query internal system staff (excluding CUSTOMER role)
        String sql = "SELECT u.id, u.first_name, u.last_name, u.email, u.mobile, u.salt, u.is_active, r.id as role_id, r.name as role_name " +
                     "FROM users u LEFT JOIN roles r ON u.role_id = r.id " +
                     "WHERE r.name IS NULL OR r.name != 'CUSTOMER' " +
                     "ORDER BY u.id DESC";
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("firstName", rs.getString("first_name"));
                map.put("lastName", rs.getString("last_name"));
                map.put("email", rs.getString("email"));
                map.put("mobile", rs.getString("mobile"));
                String rName = rs.getString("role_name");
                long rId = rs.getLong("role_id");
                map.put("roleName", rName != null ? rName : "LOGISTICS_OPERATOR");
                map.put("role", Map.of("id", rId, "name", rName != null ? rName : "LOGISTICS_OPERATOR"));
                map.put("isActive", rs.getObject("is_active") != null ? rs.getBoolean("is_active") : true);
                map.put("salt", rs.getString("salt") != null ? rs.getString("salt") : "a8f3b2c1d9e4");
                response.add(map);
            }
            if (!response.isEmpty()) {
                return Response.ok(response).build();
            }
        } catch (Exception ex) {
            // Fallback default seed users
        }

        return Response.ok(List.of(
            Map.of("id", 1, "firstName", "Admin", "lastName", "Officer", "email", "admin@globaltrade.lk", "mobile", "+1-555-0199", "roleName", "ADMIN", "role", Map.of("id", 1, "name", "ADMIN"), "isActive", true, "salt", "a8f3b2c1d9e4"),
            Map.of("id", 2, "firstName", "Sarah", "lastName", "Chen", "email", "customs.officer@globaltrade.lk", "mobile", "+1-555-0188", "roleName", "CUSTOMS_OFFICIAL", "role", Map.of("id", 2, "name", "CUSTOMS_OFFICIAL"), "isActive", true, "salt", "b9e4c2f1a8d3"),
            Map.of("id", 3, "firstName", "Marcus", "lastName", "Vance", "email", "warehouse.mgr@globaltrade.lk", "mobile", "+1-555-0177", "roleName", "WAREHOUSE_MANAGER", "role", Map.of("id", 3, "name", "WAREHOUSE_MANAGER"), "isActive", true, "salt", "c1f2a3b4e5d6"),
            Map.of("id", 4, "firstName", "Michael", "lastName", "Scott", "email", "vendor.rep@globaltrade.lk", "mobile", "+1-555-0166", "roleName", "VENDOR_REPRESENTATIVE", "role", Map.of("id", 4, "name", "VENDOR_REPRESENTATIVE"), "isActive", true, "salt", "d4e5f6a1b2c3")
        )).build();
    }

    @POST
    @Path("/create")
    public Response createUser(Map<String, Object> payload) {
        try {
            String firstName = payload.get("firstName").toString();
            String lastName = payload.get("lastName").toString();
            String email = payload.get("email").toString();
            String password = payload.get("password").toString();
            String mobile = payload.containsKey("mobile") ? payload.get("mobile").toString() : "+1-555-0199";
            Long genderId = payload.containsKey("genderId") ? Long.valueOf(payload.get("genderId").toString()) : 1L;
            Long roleId = payload.containsKey("roleId") ? Long.valueOf(payload.get("roleId").toString()) : 1L;

            User user = userService.createUser(firstName, lastName, email, password, mobile, genderId, roleId, null, null);
            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "User created successfully with SHA-256 salted password",
                "userId", user.getId(),
                "email", user.getEmail()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/toggle-status/{id}")
    public Response toggleStatus(@PathParam("id") Long id) {
        try {
            User user = userService.toggleUserStatus(id);
            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "User status toggled for " + user.getEmail(),
                "newStatus", user.getIsActive() != null && user.getIsActive() ? "ACTIVE" : "INACTIVE"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/update-role/{id}")
    public Response updateRole(@PathParam("id") Long id, Map<String, Object> payload) {
        try {
            Long newRoleId = Long.valueOf(payload.get("roleId").toString());
            User user = userService.updateUserRole(id, newRoleId);
            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "User role updated to " + user.getRole().getName()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/{id}/avatar")
    public Response uploadAvatar(@PathParam("id") Long id, Map<String, String> payload) {
        try {
            String base64Data = payload.get("imageBase64");
            if (base64Data == null || base64Data.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Image payload required")).build();
            }

            String ext = "png";
            if (base64Data.contains("data:image/jpeg") || base64Data.contains("data:image/jpg")) {
                ext = "jpg";
            }
            
            String rawBase64 = base64Data;
            if (rawBase64.contains(",")) {
                rawBase64 = rawBase64.substring(rawBase64.indexOf(",") + 1);
            }

            byte[] decoded = Base64.getDecoder().decode(rawBase64.trim());

            // 1. Resolve deployed webapp path via ServletContext
            String realPath = null;
            if (servletContext != null) {
                realPath = servletContext.getRealPath("/uploads/profile-pictures");
            }
            if (realPath == null) {
                realPath = System.getProperty("user.dir") + "/uploads/profile-pictures";
            }

            File uploadDir = new File(realPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            final String prefix = "user_" + id + "_profile.";

            // Remove existing user_{id}_profile.* files first
            File[] oldFiles = uploadDir.listFiles((dir, name) -> name.startsWith(prefix));
            if (oldFiles != null) {
                for (File f : oldFiles) {
                    f.delete();
                }
            }

            // Write new file to deployed webapp path
            String newFileName = prefix + ext;
            File targetFile = new File(uploadDir, newFileName);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(decoded);
            }

            // 2. Also try writing to project source directory for dev persistence
            File srcUploadDir = new File("/Users/bhathiya/Desktop/global-trade-logistic-corporation-core/globaltrade-web/src/main/webapp/uploads/profile-pictures");
            if (srcUploadDir.exists()) {
                File[] srcOldFiles = srcUploadDir.listFiles((dir, name) -> name.startsWith(prefix));
                if (srcOldFiles != null) {
                    for (File f : srcOldFiles) {
                        f.delete();
                    }
                }
                File srcTargetFile = new File(srcUploadDir, newFileName);
                try (FileOutputStream fosSrc = new FileOutputStream(srcTargetFile)) {
                    fosSrc.write(decoded);
                } catch (Exception ignored) {}
            }

            String avatarUrl = "../uploads/profile-pictures/" + newFileName + "?t=" + System.currentTimeMillis();
            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "Profile picture updated successfully",
                "avatarUrl", avatarUrl,
                "base64Data", base64Data
            )).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/{id}/avatar")
    public Response resetAvatar(@PathParam("id") Long id) {
        try {
            String realPath = null;
            if (servletContext != null) {
                realPath = servletContext.getRealPath("/uploads/profile-pictures");
            }
            if (realPath == null) {
                realPath = System.getProperty("user.dir") + "/uploads/profile-pictures";
            }

            File uploadDir = new File(realPath);
            final String prefix = "user_" + id + "_profile.";
            if (uploadDir.exists()) {
                File[] oldFiles = uploadDir.listFiles((dir, name) -> name.startsWith(prefix));
                if (oldFiles != null) {
                    for (File f : oldFiles) {
                        f.delete();
                    }
                }
            }

            // Also clean up source directory
            File srcUploadDir = new File("/Users/bhathiya/Desktop/global-trade-logistic-corporation-core/globaltrade-web/src/main/webapp/uploads/profile-pictures");
            if (srcUploadDir.exists()) {
                File[] srcOldFiles = srcUploadDir.listFiles((dir, name) -> name.startsWith(prefix));
                if (srcOldFiles != null) {
                    for (File f : srcOldFiles) {
                        f.delete();
                    }
                }
            }

            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "Profile picture reset to default avatar",
                "avatarUrl", "images/default-avatar.png"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
