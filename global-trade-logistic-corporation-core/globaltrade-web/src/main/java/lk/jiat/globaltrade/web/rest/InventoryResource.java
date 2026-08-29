package lk.jiat.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.globaltrade.entity.InventoryItem;
import lk.jiat.globaltrade.service.GlobalTradeStatefulCartBean;
import lk.jiat.globaltrade.service.InventoryServiceLocal;
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

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @EJB
    private InventoryServiceLocal inventoryService;

    @EJB
    private GlobalTradeStatefulCartBean statefulCartBean;

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;

    private InventoryServiceLocal getService() {
        if (inventoryService != null) return inventoryService;
        try {
            InitialContext ctx = new InitialContext();
            return (InventoryServiceLocal) ctx.lookup("java:global/globaltrade-ear-1.0/lk.jiat.globaltrade-globaltrade-ejb-1.0-SNAPSHOT/InventoryServiceBean!lk.jiat.globaltrade.service.InventoryServiceLocal");
        } catch (Exception e) {
            try {
                InitialContext ctx = new InitialContext();
                return (InventoryServiceLocal) ctx.lookup("java:module/InventoryServiceBean!lk.jiat.globaltrade.service.InventoryServiceLocal");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @GET
    public Response getAllItems() {
        List<Map<String, Object>> response = new ArrayList<>();
        String sql = "SELECT i.id, i.sku, i.name, i.description, i.unit_price, i.stock_level, i.min_stock_level, i.reorder_quantity, i.created_at, " +
                     "cat.id as category_id, cat.name as category_name, " +
                     "v.id as vendor_id, v.company_name as vendor_name, " +
                     "w.id as warehouse_id, w.name as warehouse_name " +
                     "FROM inventory_items i " +
                     "LEFT JOIN categories cat ON i.category_id = cat.id " +
                     "LEFT JOIN vendors v ON i.vendor_id = v.id " +
                     "LEFT JOIN warehouses w ON i.warehouse_id = w.id " +
                     "ORDER BY i.id DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("sku", rs.getString("sku"));
                map.put("name", rs.getString("name"));
                map.put("description", rs.getString("description"));
                map.put("unitPrice", rs.getBigDecimal("unit_price"));
                
                int stock = rs.getInt("stock_level");
                int minStock = rs.getInt("min_stock_level");
                map.put("stockLevel", stock);
                map.put("minStockLevel", minStock);
                map.put("reorderQuantity", rs.getInt("reorder_quantity"));
                map.put("createdAt", rs.getString("created_at"));
                map.put("isLowStock", stock <= minStock);

                map.put("categoryId", rs.getLong("category_id"));
                map.put("categoryName", rs.getString("category_name"));
                map.put("vendorId", rs.getLong("vendor_id"));
                map.put("vendorName", rs.getString("vendor_name"));
                map.put("warehouseId", rs.getLong("warehouse_id"));
                map.put("warehouseName", rs.getString("warehouse_name"));

                response.add(map);
            }
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to fetch inventory items: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/categories")
    public Response getCategories() {
        List<Map<String, Object>> response = new ArrayList<>();
        String sql = "SELECT id, name, description FROM categories ORDER BY name ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                response.add(Map.of(
                    "id", rs.getLong("id"),
                    "name", rs.getString("name"),
                    "description", rs.getString("description") != null ? rs.getString("description") : ""
                ));
            }
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/low-stock")
    public Response getLowStockItems() {
        try {
            InventoryServiceLocal service = getService();
            List<InventoryItem> items = service != null ? service.getLowStockItems() : new ArrayList<>();
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/create")
    public Response createItem(Map<String, Object> payload) {
        try {
            String sku = payload.get("sku").toString().trim();
            String name = payload.get("name").toString().trim();
            String description = payload.getOrDefault("description", "").toString();
            BigDecimal unitPrice = new BigDecimal(payload.get("unitPrice").toString());
            int stockLevel = Integer.parseInt(payload.getOrDefault("stockLevel", "0").toString());
            int minStockLevel = Integer.parseInt(payload.getOrDefault("minStockLevel", "10").toString());
            int reorderQuantity = Integer.parseInt(payload.getOrDefault("reorderQuantity", "50").toString());
            Long categoryId = Long.valueOf(payload.getOrDefault("categoryId", "1").toString());
            Long vendorId = Long.valueOf(payload.getOrDefault("vendorId", "1").toString());
            Long warehouseId = Long.valueOf(payload.getOrDefault("warehouseId", "1").toString());

            InventoryServiceLocal service = getService();
            if (service != null) {
                InventoryItem created = service.createInventoryItem(sku, name, description, unitPrice, stockLevel, minStockLevel, reorderQuantity, categoryId, vendorId, warehouseId);
                return Response.ok(Map.of("status", "SUCCESS", "message", "Product registered successfully in MySQL", "itemId", created.getId(), "sku", created.getSku())).build();
            } else {
                String insertSql = "INSERT INTO inventory_items (sku, name, description, unit_price, stock_level, min_stock_level, reorder_quantity, category_id, vendor_id, warehouse_id) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, sku);
                    ps.setString(2, name);
                    ps.setString(3, description);
                    ps.setBigDecimal(4, unitPrice);
                    ps.setInt(5, stockLevel);
                    ps.setInt(6, minStockLevel);
                    ps.setInt(7, reorderQuantity);
                    ps.setLong(8, categoryId);
                    ps.setLong(9, vendorId);
                    ps.setLong(10, warehouseId);
                    ps.executeUpdate();
                    long newId = 0;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) newId = rs.getLong(1);
                    }
                    return Response.ok(Map.of("status", "SUCCESS", "message", "Product registered successfully in MySQL", "itemId", newId, "sku", sku)).build();
                }
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/update/{id}")
    public Response updateItem(@PathParam("id") Long id, Map<String, Object> payload) {
        try {
            String sku = payload.get("sku").toString().trim();
            String name = payload.get("name").toString().trim();
            String description = payload.getOrDefault("description", "").toString();
            BigDecimal unitPrice = new BigDecimal(payload.get("unitPrice").toString());
            int stockLevel = Integer.parseInt(payload.get("stockLevel").toString());
            int minStockLevel = Integer.parseInt(payload.get("minStockLevel").toString());
            int reorderQuantity = Integer.parseInt(payload.getOrDefault("reorderQuantity", "50").toString());
            Long categoryId = Long.valueOf(payload.getOrDefault("categoryId", "1").toString());
            Long vendorId = Long.valueOf(payload.getOrDefault("vendorId", "1").toString());
            Long warehouseId = Long.valueOf(payload.getOrDefault("warehouseId", "1").toString());

            InventoryServiceLocal service = getService();
            if (service != null) {
                InventoryItem updated = service.updateInventoryItem(id, sku, name, description, unitPrice, stockLevel, minStockLevel, reorderQuantity, categoryId, vendorId, warehouseId);
                return Response.ok(Map.of("status", "SUCCESS", "message", "Product details updated successfully", "itemId", updated.getId())).build();
            } else {
                String updateSql = "UPDATE inventory_items SET sku=?, name=?, description=?, unit_price=?, stock_level=?, min_stock_level=?, reorder_quantity=?, category_id=?, vendor_id=?, warehouse_id=? WHERE id=?";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, sku);
                    ps.setString(2, name);
                    ps.setString(3, description);
                    ps.setBigDecimal(4, unitPrice);
                    ps.setInt(5, stockLevel);
                    ps.setInt(6, minStockLevel);
                    ps.setInt(7, reorderQuantity);
                    ps.setLong(8, categoryId);
                    ps.setLong(9, vendorId);
                    ps.setLong(10, warehouseId);
                    ps.setLong(11, id);
                    ps.executeUpdate();
                    return Response.ok(Map.of("status", "SUCCESS", "message", "Product details updated successfully", "itemId", id)).build();
                }
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/adjust-stock")
    public Response adjustStock(Map<String, Object> payload) {
        try {
            Long itemId = Long.valueOf(payload.get("itemId").toString());
            int delta = Integer.parseInt(payload.get("delta").toString());

            InventoryServiceLocal service = getService();
            if (service != null) {
                service.adjustStockLevel(itemId, delta);
            } else {
                String updateSql = "UPDATE inventory_items SET stock_level = GREATEST(0, stock_level + ?) WHERE id = ?";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, delta);
                    ps.setLong(2, itemId);
                    ps.executeUpdate();
                }
            }
            return Response.ok(Map.of("status", "SUCCESS", "message", "Stock level updated by " + delta)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/delete/{id}")
    public Response deleteItem(@PathParam("id") Long id) {
        try {
            InventoryServiceLocal service = getService();
            if (service != null) {
                service.deleteInventoryItem(id);
            } else {
                String deleteSql = "DELETE FROM inventory_items WHERE id = ?";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
            }
            return Response.ok(Map.of("status", "SUCCESS", "message", "Product deleted successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/cart")
    public Response getCartItems() {
        try {
            if (statefulCartBean != null && statefulCartBean.getDraftItems() != null) {
                List<Map<String, Object>> result = new ArrayList<>();
                List<lk.jiat.globaltrade.entity.InventoryItem> items = statefulCartBean.getDraftItems();
                List<Integer> qtys = statefulCartBean.getDraftQuantities();
                for (int i = 0; i < items.size(); i++) {
                    lk.jiat.globaltrade.entity.InventoryItem item = items.get(i);
                    int qty = i < qtys.size() ? qtys.get(i) : 1;
                    Map<String, Object> map = new HashMap<>();
                    map.put("itemId", item.getId());
                    map.put("sku", item.getSku());
                    map.put("name", item.getName());
                    map.put("unitPrice", item.getUnitPrice());
                    map.put("quantity", qty);
                    result.add(map);
                }
                return Response.ok(result).build();
            }
            return Response.ok(List.of()).build();
        } catch (Exception e) {
            return Response.ok(List.of()).build();
        }
    }
}
