package lk.jiat.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/vendors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VendorResource {

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;

    @GET
    public Response getAllVendors() {
        List<Map<String, Object>> response = new ArrayList<>();
        String sql = "SELECT v.id, v.vendor_code, v.company_name, v.contact_email, v.contact_phone, v.rating, " +
                     "c.id as country_id, c.name as country_name, c.iso_code_2 as country_code, " +
                     "s.code as status_code, s.description as status_desc " +
                     "FROM vendors v " +
                     "LEFT JOIN countries c ON v.country_id = c.id " +
                     "LEFT JOIN vendor_compliance_statuses s ON v.compliance_status_id = s.id " +
                     "ORDER BY v.id DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("vendorCode", rs.getString("vendor_code"));
                map.put("companyName", rs.getString("company_name"));
                map.put("contactEmail", rs.getString("contact_email"));
                map.put("contactPhone", rs.getString("contact_phone"));
                map.put("rating", rs.getDouble("rating"));
                map.put("countryId", rs.getLong("country_id"));
                map.put("countryName", rs.getString("country_name") != null ? rs.getString("country_name") : "Global");
                map.put("countryCode", rs.getString("country_code"));
                map.put("complianceStatusCode", rs.getString("status_code") != null ? rs.getString("status_code") : "COMPLIANT");
                map.put("complianceStatusDescription", rs.getString("status_desc"));
                response.add(map);
            }
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to fetch vendors from MySQL: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getVendorById(@PathParam("id") Long id) {
        String sql = "SELECT v.id, v.vendor_code, v.company_name, v.contact_email, v.contact_phone, v.rating, " +
                     "c.id as country_id, c.name as country_name, s.code as status_code " +
                     "FROM vendors v " +
                     "LEFT JOIN countries c ON v.country_id = c.id " +
                     "LEFT JOIN vendor_compliance_statuses s ON v.compliance_status_id = s.id " +
                     "WHERE v.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getLong("id"));
                    map.put("vendorCode", rs.getString("vendor_code"));
                    map.put("companyName", rs.getString("company_name"));
                    map.put("contactEmail", rs.getString("contact_email"));
                    map.put("contactPhone", rs.getString("contact_phone"));
                    map.put("rating", rs.getDouble("rating"));
                    map.put("countryId", rs.getLong("country_id"));
                    map.put("countryName", rs.getString("country_name"));
                    map.put("complianceStatusCode", rs.getString("status_code"));
                    return Response.ok(map).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Vendor not found")).build();
                }
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    public Response createVendor(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("companyName")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Company name is required")).build();
        }

        String companyName = payload.get("companyName").toString().trim();
        String vendorCode = payload.containsKey("vendorCode") && !payload.get("vendorCode").toString().trim().isEmpty() 
                ? payload.get("vendorCode").toString().trim() 
                : "VND-" + (System.currentTimeMillis() % 100000);
        String contactEmail = payload.getOrDefault("contactEmail", "contact@vendor.com").toString().trim();
        String contactPhone = payload.getOrDefault("contactPhone", "+1-800-555-0100").toString().trim();
        long countryId = payload.containsKey("countryId") ? Long.parseLong(payload.get("countryId").toString()) : 1L;
        String statusCode = payload.getOrDefault("complianceStatus", "COMPLIANT").toString();

        String sql = "INSERT INTO vendors (vendor_code, company_name, country_id, contact_email, contact_phone, rating, compliance_status_id) " +
                     "VALUES (?, ?, ?, ?, ?, 5.00, (SELECT id FROM vendor_compliance_statuses WHERE code = ? LIMIT 1))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, vendorCode);
            ps.setString(2, companyName);
            ps.setLong(3, countryId);
            ps.setString(4, contactEmail);
            ps.setString(5, contactPhone);
            ps.setString(6, statusCode);

            ps.executeUpdate();
            long newId = 0;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) newId = rs.getLong(1);
            }

            return Response.ok(Map.of(
                "status", "SUCCESS",
                "id", newId,
                "vendorCode", vendorCode,
                "message", "Registered vendor '" + companyName + "' in MySQL database successfully!"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to insert vendor into MySQL: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateVendor(@PathParam("id") Long id, Map<String, Object> payload) {
        String companyName = payload.getOrDefault("companyName", "").toString().trim();
        String contactEmail = payload.getOrDefault("contactEmail", "").toString().trim();
        String contactPhone = payload.getOrDefault("contactPhone", "").toString().trim();
        long countryId = payload.containsKey("countryId") ? Long.parseLong(payload.get("countryId").toString()) : 1L;
        String statusCode = payload.getOrDefault("complianceStatus", "COMPLIANT").toString();

        String sql = "UPDATE vendors SET company_name = ?, contact_email = ?, contact_phone = ?, country_id = ?, " +
                     "compliance_status_id = (SELECT id FROM vendor_compliance_statuses WHERE code = ? LIMIT 1) " +
                     "WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, companyName);
            ps.setString(2, contactEmail);
            ps.setString(3, contactPhone);
            ps.setLong(4, countryId);
            ps.setString(5, statusCode);
            ps.setLong(6, id);

            ps.executeUpdate();
            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "Updated vendor '" + companyName + "' in MySQL database successfully!"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to update vendor in MySQL: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteVendor(@PathParam("id") Long id) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement("UPDATE users SET vendor_id = NULL WHERE vendor_id = ?")) {
                    ps1.setLong(1, id);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM vendor_evaluations WHERE vendor_id = ?")) {
                    ps2.setLong(1, id);
                    ps2.executeUpdate();
                }
                try (PreparedStatement ps3 = conn.prepareStatement("DELETE FROM vendors WHERE id = ?")) {
                    ps3.setLong(1, id);
                    ps3.executeUpdate();
                }

                conn.commit();
                return Response.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Deleted vendor ID " + id + " from MySQL database successfully!"
                )).build();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to delete vendor from MySQL: " + e.getMessage()))
                    .build();
        }
    }
}
