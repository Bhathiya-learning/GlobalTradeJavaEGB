package lk.jiat.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.globaltrade.entity.CustomsDeclaration;
import lk.jiat.globaltrade.service.CustomsClearanceServiceLocal;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/customs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomsResource {

    @EJB
    private CustomsClearanceServiceLocal customsService;

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;

    private CustomsClearanceServiceLocal getService() {
        if (customsService != null) {
            return customsService;
        }
        try {
            InitialContext ctx = new InitialContext();
            return (CustomsClearanceServiceLocal) ctx.lookup("java:global/globaltrade-ear-1.0/lk.jiat.globaltrade-globaltrade-ejb-1.0-SNAPSHOT/CustomsClearanceServiceBean!lk.jiat.globaltrade.service.CustomsClearanceServiceLocal");
        } catch (Exception e) {
            return null;
        }
    }

    @GET
    public Response getAllDeclarations() {
        List<Map<String, Object>> resultList = new ArrayList<>();
        String sql = "SELECT d.id, d.declaration_number, d.shipment_id, s.tracking_number, " +
                     "cs.code AS status_code, cs.name AS status_name, " +
                     "d.declaration_date, d.duty_amount, d.hs_tariff_code_id, " +
                     "hs.hs_code, hs.description AS hs_description, hs.default_duty_rate_pct, " +
                     "d.inspected_by_user_id, CONCAT(u.first_name, ' ', u.last_name) AS inspector_name, " +
                     "d.compliance_notes " +
                     "FROM customs_declarations d " +
                     "JOIN shipments s ON d.shipment_id = s.id " +
                     "JOIN customs_statuses cs ON d.customs_status_id = cs.id " +
                     "JOIN hs_tariff_codes hs ON d.hs_tariff_code_id = hs.id " +
                     "LEFT JOIN users u ON d.inspected_by_user_id = u.id " +
                     "ORDER BY d.id DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("declarationNumber", rs.getString("declaration_number"));
                map.put("shipmentId", rs.getLong("shipment_id"));
                map.put("trackingNumber", rs.getString("tracking_number"));
                map.put("statusCode", rs.getString("status_code"));
                map.put("statusName", rs.getString("status_name"));
                map.put("declarationDate", rs.getTimestamp("declaration_date") != null ? rs.getTimestamp("declaration_date").toString() : "");
                map.put("dutyAmount", rs.getBigDecimal("duty_amount"));
                map.put("hsTariffCodeId", rs.getLong("hs_tariff_code_id"));
                map.put("hsCode", rs.getString("hs_code"));
                map.put("hsDescription", rs.getString("hs_description"));
                map.put("dutyRatePct", rs.getBigDecimal("default_duty_rate_pct"));
                map.put("inspectorUserId", rs.getObject("inspected_by_user_id"));
                map.put("inspectorName", rs.getString("inspector_name") != null ? rs.getString("inspector_name") : "Pending Inspector");
                map.put("complianceNotes", rs.getString("compliance_notes"));
                resultList.add(map);
            }
            return Response.ok(resultList).build();
        } catch (Exception e) {
            // EJB fallback
            CustomsClearanceServiceLocal service = getService();
            if (service != null) {
                try {
                    List<CustomsDeclaration> list = service.getAllDeclarations();
                    return Response.ok(list).build();
                } catch (Exception ex) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", ex.getMessage())).build();
                }
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/hs-codes")
    public Response getHsTariffCodes() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT id, hs_code, description, default_duty_rate_pct FROM hs_tariff_codes ORDER BY id ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("hsCode", rs.getString("hs_code"));
                map.put("description", rs.getString("description"));
                map.put("dutyRatePct", rs.getBigDecimal("default_duty_rate_pct"));
                result.add(map);
            }
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/create")
    public Response createDeclaration(Map<String, Object> payload) {
        try {
            Long shipmentId = Long.valueOf(payload.get("shipmentId").toString());
            Long hsCodeId = Long.valueOf(payload.get("hsTariffCodeId").toString());
            BigDecimal duty = new BigDecimal(payload.getOrDefault("dutyAmount", "0.00").toString());
            String notes = payload.getOrDefault("notes", "Submitted via WCO Customs Portal").toString();

            String declNum = "DEC-CUSTOMS-" + System.currentTimeMillis() % 100000;
            if (payload.containsKey("declarationNumber") && payload.get("declarationNumber") != null && !payload.get("declarationNumber").toString().isEmpty()) {
                declNum = payload.get("declarationNumber").toString();
            }

            String sql = "INSERT INTO customs_declarations (declaration_number, shipment_id, customs_status_id, duty_amount, hs_tariff_code_id, compliance_notes) " +
                         "VALUES (?, ?, 1, ?, ?, ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, declNum);
                ps.setLong(2, shipmentId);
                ps.setBigDecimal(3, duty);
                ps.setLong(4, hsCodeId);
                ps.setString(5, notes);
                ps.executeUpdate();
            }

            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "Customs border declaration " + declNum + " created successfully in MySQL database."
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/approve")
    public Response approve(Map<String, Object> payload) {
        try {
            Long declId = Long.valueOf(payload.get("declarationId").toString());
            Long inspectorId = payload.containsKey("inspectorUserId") && payload.get("inspectorUserId") != null ? Long.valueOf(payload.get("inspectorUserId").toString()) : 4L;
            Long statusId = payload.containsKey("statusId") && payload.get("statusId") != null ? Long.valueOf(payload.get("statusId").toString()) : 2L;
            BigDecimal duty = new BigDecimal(payload.getOrDefault("dutyAmount", "0.00").toString());
            String notes = payload.getOrDefault("notes", "Customs declaration verified and approved under WCO standard.").toString();

            String updateSql = "UPDATE customs_declarations SET customs_status_id = ?, duty_amount = ?, inspected_by_user_id = ?, compliance_notes = ? WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setLong(1, statusId);
                ps.setBigDecimal(2, duty);
                ps.setLong(3, inspectorId);
                ps.setString(4, notes);
                ps.setLong(5, declId);
                ps.executeUpdate();
            }

            // Sync shipment status: if approved (statusId == 2), release hold (status_id = 2 / IN_TRANSIT)
            long shipmentStatusId = (statusId == 2L) ? 2L : 3L;
            String updateShipmentSql = "UPDATE shipments s JOIN customs_declarations d ON s.id = d.shipment_id SET s.status_id = ? WHERE d.id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateShipmentSql)) {
                ps.setLong(1, shipmentStatusId);
                ps.setLong(2, declId);
                ps.executeUpdate();
            }

            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "Declaration #" + declId + " updated successfully in MySQL with duty $" + duty
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/reject")
    public Response reject(Map<String, Object> payload) {
        try {
            Long declId = Long.valueOf(payload.get("declarationId").toString());
            Long inspectorId = payload.containsKey("inspectorUserId") && payload.get("inspectorUserId") != null ? Long.valueOf(payload.get("inspectorUserId").toString()) : 4L;
            String reason = payload.getOrDefault("reason", "Improper tariff documentation or border compliance failure").toString();

            String updateSql = "UPDATE customs_declarations SET customs_status_id = 3, inspected_by_user_id = ?, compliance_notes = ? WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setLong(1, inspectorId);
                ps.setString(2, "REJECTED: " + reason);
                ps.setLong(3, declId);
                ps.executeUpdate();
            }

            // Also flag shipment status as CUSTOMS_HOLD (status_id = 3)
            String updateShipmentSql = "UPDATE shipments s JOIN customs_declarations d ON s.id = d.shipment_id SET s.status_id = 3 WHERE d.id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateShipmentSql)) {
                ps.setLong(1, declId);
                ps.executeUpdate();
            }

            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "Declaration #" + declId + " REJECTED by border inspector. Shipment placed on Customs Hold."
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
