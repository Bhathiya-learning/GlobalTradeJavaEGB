package lk.jiat.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.globaltrade.entity.Shipment;
import lk.jiat.globaltrade.service.ShipmentServiceLocal;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/shipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShipmentResource {

    @EJB
    private ShipmentServiceLocal shipmentService;

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;

    @jakarta.annotation.PostConstruct
    public void seedDiverseRoutes() {
        try (Connection conn = dataSource.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM shipments WHERE tracking_number = 'GTL-2026-SGJP99'";
            try (PreparedStatement ps = conn.prepareStatement(checkSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO shipments (tracking_number, origin_warehouse_id, destination_address_id, carrier_id, status_id, customer_id, dispatch_date, estimated_delivery, created_by_user_id) VALUES " +
                        "('GTL-2026-SGJP99', 3, 4, 2, 2, 1, '2026-08-24 10:00:00', '2026-08-31 18:00:00', 5), " +
                        "('GTL-2026-EULN88', 2, 7, 3, 2, 1, '2026-08-25 08:30:00', '2026-09-02 12:00:00', 5), " +
                        "('GTL-2026-LKNL77', 4, 5, 1, 2, 1, '2026-08-23 14:15:00', '2026-09-05 16:00:00', 5), " +
                        "('GTL-2026-EUAE66', 2, 6, 4, 2, 1, '2026-08-26 09:45:00', '2026-09-01 10:00:00', 5)";
                    try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                        psIns.executeUpdate();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private ShipmentServiceLocal getService() {
        if (shipmentService != null) return shipmentService;
        try {
            InitialContext ctx = new InitialContext();
            return (ShipmentServiceLocal) ctx.lookup("java:global/globaltrade-ear-1.0/lk.jiat.globaltrade-globaltrade-ejb-1.0-SNAPSHOT/ShipmentServiceBean!lk.jiat.globaltrade.service.ShipmentServiceLocal");
        } catch (Exception e) {
            try {
                InitialContext ctx = new InitialContext();
                return (ShipmentServiceLocal) ctx.lookup("java:module/ShipmentServiceBean!lk.jiat.globaltrade.service.ShipmentServiceLocal");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @GET
    public Response getAllShipments(@QueryParam("userId") Long userId) {
        List<Map<String, Object>> response = new ArrayList<>();
        String sql = "SELECT s.id, s.tracking_number, s.dispatch_date, s.estimated_delivery, s.actual_delivery, s.created_at, " +
                     "w.name as origin_warehouse_name, " +
                     "ct.name as dest_city, " +
                     "c.id as carrier_id, c.company_name as carrier_name, " +
                     "st.id as status_id, st.code as status_code, st.name as status_name, " +
                     "u.id as user_id, u.first_name, u.last_name, u.email as user_email, " +
                     "cust.id as cust_id, cust.customer_code, cust.company_name as cust_company, cust.eori_number as cust_eori " +
                     "FROM shipments s " +
                     "LEFT JOIN warehouses w ON s.origin_warehouse_id = w.id " +
                     "LEFT JOIN addresses a ON s.destination_address_id = a.id " +
                     "LEFT JOIN cities ct ON a.city_id = ct.id " +
                     "LEFT JOIN carriers c ON s.carrier_id = c.id " +
                     "LEFT JOIN shipment_statuses st ON s.status_id = st.id " +
                     "LEFT JOIN users u ON s.created_by_user_id = u.id " +
                     "LEFT JOIN customers cust ON s.customer_id = cust.id OR s.created_by_user_id = cust.user_id " +
                     "WHERE (? IS NULL OR s.created_by_user_id = ? OR s.customer_id = (SELECT id FROM customers WHERE user_id = ? LIMIT 1)) " +
                     "ORDER BY s.id DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setLong(1, userId);
                ps.setLong(2, userId);
                ps.setLong(3, userId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
                ps.setNull(2, java.sql.Types.BIGINT);
                ps.setNull(3, java.sql.Types.BIGINT);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getLong("id"));
                    map.put("userId", rs.getObject("user_id") != null ? rs.getLong("user_id") : null);
                    map.put("trackingNumber", rs.getString("tracking_number"));
                    map.put("dispatchDate", rs.getString("dispatch_date"));
                    map.put("estimatedDelivery", rs.getString("estimated_delivery"));
                    map.put("actualDelivery", rs.getString("actual_delivery"));
                    map.put("createdAt", rs.getString("created_at"));
                    map.put("originWarehouseName", rs.getString("origin_warehouse_name"));
                    map.put("destinationCity", rs.getString("dest_city"));

                    // Carrier Vendor info
                    Map<String, Object> carrierMap = new HashMap<>();
                    carrierMap.put("id", rs.getLong("carrier_id"));
                    carrierMap.put("companyName", rs.getString("carrier_name"));
                    map.put("carrier", carrierMap);

                    // Status info
                    Map<String, Object> statusMap = new HashMap<>();
                    statusMap.put("id", rs.getLong("status_id"));
                    statusMap.put("code", rs.getString("status_code"));
                    statusMap.put("name", rs.getString("status_name"));
                    map.put("status", statusMap);

                    // Created By User info
                    if (rs.getObject("user_id") != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", rs.getLong("user_id"));
                        userMap.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                        userMap.put("email", rs.getString("user_email"));
                        map.put("createdByUser", userMap);
                    }

                    // Customer Entity info
                    if (rs.getObject("cust_id") != null || rs.getObject("user_id") != null) {
                        Map<String, Object> custMap = new HashMap<>();
                        custMap.put("id", rs.getObject("cust_id") != null ? rs.getLong("cust_id") : rs.getLong("user_id"));
                        custMap.put("customerCode", rs.getString("customer_code") != null ? rs.getString("customer_code") : "CUST-US-" + rs.getLong("user_id"));
                        custMap.put("companyName", rs.getString("cust_company") != null ? rs.getString("cust_company") : "Apex Global Forwarding LLC");
                        custMap.put("fullName", rs.getString("first_name") + " " + rs.getString("last_name"));
                        custMap.put("email", rs.getString("user_email"));
                        map.put("customer", custMap);
                    }

                    response.add(map);
                }
                return Response.ok(response).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to fetch shipments: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/customer/{userId}")
    public Response getCustomerShipments(@PathParam("userId") Long userId) {
        return getAllShipments(userId);
    }

    @GET
    @Path("/{id}")
    public Response getShipmentById(@PathParam("id") Long id) {
        String sql = "SELECT s.id, s.tracking_number, s.dispatch_date, s.estimated_delivery, s.actual_delivery, s.created_at, " +
                     "w.name as origin_warehouse_name, " +
                     "ct.name as dest_city, " +
                     "c.id as carrier_id, c.company_name as carrier_name, " +
                     "st.id as status_id, st.code as status_code, st.name as status_name, " +
                     "u.id as user_id, u.first_name, u.last_name, u.email as user_email, " +
                     "cust.id as cust_id, cust.customer_code, cust.company_name as cust_company " +
                     "FROM shipments s " +
                     "LEFT JOIN warehouses w ON s.origin_warehouse_id = w.id " +
                     "LEFT JOIN addresses a ON s.destination_address_id = a.id " +
                     "LEFT JOIN cities ct ON a.city_id = ct.id " +
                     "LEFT JOIN carriers c ON s.carrier_id = c.id " +
                     "LEFT JOIN shipment_statuses st ON s.status_id = st.id " +
                     "LEFT JOIN users u ON s.created_by_user_id = u.id " +
                     "LEFT JOIN customers cust ON s.customer_id = cust.id OR s.created_by_user_id = cust.user_id " +
                     "WHERE s.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getLong("id"));
                    map.put("userId", rs.getObject("user_id") != null ? rs.getLong("user_id") : null);
                    map.put("trackingNumber", rs.getString("tracking_number"));
                    map.put("dispatchDate", rs.getString("dispatch_date"));
                    map.put("estimatedDelivery", rs.getString("estimated_delivery"));
                    map.put("actualDelivery", rs.getString("actual_delivery"));
                    map.put("createdAt", rs.getString("created_at"));
                    map.put("originWarehouseName", rs.getString("origin_warehouse_name"));
                    map.put("destinationCity", rs.getString("dest_city"));

                    Map<String, Object> carrierMap = new HashMap<>();
                    carrierMap.put("id", rs.getLong("carrier_id"));
                    carrierMap.put("companyName", rs.getString("carrier_name"));
                    map.put("carrier", carrierMap);

                    Map<String, Object> statusMap = new HashMap<>();
                    statusMap.put("id", rs.getLong("status_id"));
                    statusMap.put("code", rs.getString("status_code"));
                    statusMap.put("name", rs.getString("status_name"));
                    map.put("status", statusMap);

                    if (rs.getObject("user_id") != null) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", rs.getLong("user_id"));
                        userMap.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                        userMap.put("email", rs.getString("user_email"));
                        map.put("createdByUser", userMap);
                    }

                    if (rs.getObject("cust_id") != null || rs.getObject("user_id") != null) {
                        Map<String, Object> custMap = new HashMap<>();
                        custMap.put("id", rs.getObject("cust_id") != null ? rs.getLong("cust_id") : rs.getLong("user_id"));
                        custMap.put("customerCode", rs.getString("customer_code") != null ? rs.getString("customer_code") : "CUST-US-" + rs.getLong("user_id"));
                        custMap.put("companyName", rs.getString("cust_company") != null ? rs.getString("cust_company") : "Apex Global Forwarding LLC");
                        custMap.put("fullName", rs.getString("first_name") + " " + rs.getString("last_name"));
                        custMap.put("email", rs.getString("user_email"));
                        map.put("customer", custMap);
                    }

                    return Response.ok(map).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Shipment not found")).build();
                }
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/create")
    @SuppressWarnings("unchecked")
    public Response createShipment(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("originWarehouseId") || !payload.containsKey("destinationAddressId")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Origin warehouse and destination address are required")).build();
        }

        try {
            Long originWarehouseId = Long.valueOf(payload.get("originWarehouseId").toString());
            Long destinationAddressId = Long.valueOf(payload.get("destinationAddressId").toString());
            Long carrierId = payload.containsKey("carrierId") && payload.get("carrierId") != null ? Long.valueOf(payload.get("carrierId").toString()) : 1L;
            Long userId = payload.containsKey("userId") && payload.get("userId") != null ? Long.valueOf(payload.get("userId").toString()) : 5L;

            // Ban inactive users from placing orders / dispatching shipments
            String checkUserActiveSql = "SELECT is_active, first_name, email FROM users WHERE id = ? OR id = (SELECT user_id FROM customers WHERE id = ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement psActive = conn.prepareStatement(checkUserActiveSql)) {
                psActive.setLong(1, userId);
                psActive.setLong(2, userId);
                try (ResultSet rsActive = psActive.executeQuery()) {
                    if (rsActive.next()) {
                        boolean isActive = rsActive.getBoolean("is_active");
                        if (!isActive) {
                            return Response.status(Response.Status.FORBIDDEN).entity(Map.of(
                                "error", "⛔ Order placement blocked! Your account (" + rsActive.getString("email") + ") has been suspended or deactivated by system administration."
                            )).build();
                        }
                    }
                }
            }

            List<Long> itemIds = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();

            if (payload.containsKey("items") && payload.get("items") instanceof List) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
                for (Map<String, Object> item : items) {
                    itemIds.add(Long.valueOf(item.get("itemId").toString()));
                    quantities.add(Integer.valueOf(item.get("quantity").toString()));
                }
            } else {
                itemIds.add(1L);
                quantities.add(100);
            }

            // Verify stock level for each requested item
            String checkStockSql = "SELECT name, sku, stock_level FROM inventory_items WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement psCheck = conn.prepareStatement(checkStockSql)) {
                for (int i = 0; i < itemIds.size(); i++) {
                    psCheck.setLong(1, itemIds.get(i));
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            int available = rsCheck.getInt("stock_level");
                            int requested = quantities.get(i);
                            if (requested > available) {
                                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of(
                                    "error", String.format("Insufficient stock level for SKU %s (%s). Available: %d units, Requested: %d units.", rsCheck.getString("sku"), rsCheck.getString("name"), available, requested)
                                )).build();
                            }
                        }
                    }
                }
            }

            // Clean JDBC insertion updating both created_by_user_id AND customer_id
            String trackingNum = "GTL-2026-" + (System.currentTimeMillis() % 100000);
            String insertSql = "INSERT INTO shipments (tracking_number, origin_warehouse_id, destination_address_id, carrier_id, status_id, created_by_user_id, customer_id, estimated_delivery) " +
                               "VALUES (?, ?, ?, ?, 1, ?, (SELECT id FROM customers WHERE user_id = ? LIMIT 1), NOW() + INTERVAL 7 DAY)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, trackingNum);
                ps.setLong(2, originWarehouseId);
                ps.setLong(3, destinationAddressId);
                ps.setLong(4, carrierId);
                ps.setLong(5, userId);
                ps.setLong(6, userId);
                ps.executeUpdate();
                long newId = 0;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) newId = rs.getLong(1);
                }
                return Response.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Shipment booking request created successfully and saved in MySQL under Customer User ID " + userId,
                    "trackingNumber", trackingNum,
                    "shipmentId", newId
                )).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
