package lk.jiat.globaltrade.service;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.jiat.globaltrade.annotation.LogisticsAudited;
import lk.jiat.globaltrade.entity.InventoryItem;
import lk.jiat.globaltrade.exception.GlobalTradeException;
import lk.jiat.globaltrade.interceptor.LogisticsAuditInterceptor;

import java.util.List;
import java.util.logging.Logger;

@Stateless
@DeclareRoles({"ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "VENDOR_REPRESENTATIVE"})
@Interceptors({LogisticsAuditInterceptor.class})
public class InventoryServiceBean implements InventoryServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(InventoryServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<InventoryItem> getAllInventoryItems() {
        return em.createQuery("SELECT i FROM InventoryItem i ORDER BY i.name ASC", InventoryItem.class).getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<InventoryItem> getLowStockItems() {
        return em.createQuery("SELECT i FROM InventoryItem i WHERE i.stockLevel <= i.minStockLevel", InventoryItem.class).getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public InventoryItem getItemBySku(String sku) {
        List<InventoryItem> items = em.createQuery("SELECT i FROM InventoryItem i WHERE i.sku = :sku", InventoryItem.class)
                                      .setParameter("sku", sku)
                                      .getResultList();
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public InventoryItem getInventoryItemById(Long id) {
        return em.find(InventoryItem.class, id);
    }

    @Override
    @LogisticsAudited(actionCode = "INVENTORY_CREATE", targetEntity = "InventoryItem")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem createInventoryItem(String sku, String name, String description, java.math.BigDecimal unitPrice, int stockLevel, int minStockLevel, int reorderQuantity, Long categoryId, Long vendorId, Long warehouseId) throws GlobalTradeException {
        lk.jiat.globaltrade.entity.Category category = em.find(lk.jiat.globaltrade.entity.Category.class, categoryId != null ? categoryId : 1L);
        lk.jiat.globaltrade.entity.Vendor vendor = em.find(lk.jiat.globaltrade.entity.Vendor.class, vendorId != null ? vendorId : 1L);
        lk.jiat.globaltrade.entity.Warehouse warehouse = em.find(lk.jiat.globaltrade.entity.Warehouse.class, warehouseId != null ? warehouseId : 1L);

        if (category == null || vendor == null || warehouse == null) {
            throw new GlobalTradeException("Invalid Category, Vendor, or Warehouse reference for inventory item.");
        }

        InventoryItem item = new InventoryItem(null, sku, name, description, unitPrice, stockLevel, minStockLevel, reorderQuantity, category, vendor, warehouse);
        em.persist(item);
        LOGGER.info(String.format("[INVENTORY CREATED] Created new SKU %s - %s", sku, name));
        return item;
    }

    @Override
    @LogisticsAudited(actionCode = "INVENTORY_UPDATE", targetEntity = "InventoryItem")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem updateInventoryItem(Long id, String sku, String name, String description, java.math.BigDecimal unitPrice, int stockLevel, int minStockLevel, int reorderQuantity, Long categoryId, Long vendorId, Long warehouseId) throws GlobalTradeException {
        InventoryItem item = em.find(InventoryItem.class, id);
        if (item == null) {
            throw new GlobalTradeException("Inventory item not found for ID: " + id);
        }

        lk.jiat.globaltrade.entity.Category category = em.find(lk.jiat.globaltrade.entity.Category.class, categoryId != null ? categoryId : 1L);
        lk.jiat.globaltrade.entity.Vendor vendor = em.find(lk.jiat.globaltrade.entity.Vendor.class, vendorId != null ? vendorId : 1L);
        lk.jiat.globaltrade.entity.Warehouse warehouse = em.find(lk.jiat.globaltrade.entity.Warehouse.class, warehouseId != null ? warehouseId : 1L);

        item.setSku(sku);
        item.setName(name);
        item.setDescription(description);
        item.setUnitPrice(unitPrice);
        item.setStockLevel(stockLevel);
        item.setMinStockLevel(minStockLevel);
        item.setReorderQuantity(reorderQuantity);
        if (category != null) item.setCategory(category);
        if (vendor != null) item.setVendor(vendor);
        if (warehouse != null) item.setWarehouse(warehouse);

        em.merge(item);
        LOGGER.info(String.format("[INVENTORY UPDATED] Updated SKU %s (ID %d)", sku, id));
        return item;
    }

    @Override
    @LogisticsAudited(actionCode = "INVENTORY_STOCK_UPDATE", targetEntity = "InventoryItem")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void adjustStockLevel(Long itemId, int delta) {
        InventoryItem item = em.find(InventoryItem.class, itemId);
        if (item != null) {
            int newStock = Math.max(0, item.getStockLevel() + delta);
            item.setStockLevel(newStock);
            em.merge(item);
            LOGGER.info(String.format("[INVENTORY STOCK ADJUSTED] Item %s (%s) stock level adjusted by %d -> new total %d", item.getName(), item.getSku(), delta, newStock));
        }
    }

    @Override
    @LogisticsAudited(actionCode = "INVENTORY_DELETE", targetEntity = "InventoryItem")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteInventoryItem(Long id) throws GlobalTradeException {
        InventoryItem item = em.find(InventoryItem.class, id);
        if (item != null) {
            em.remove(item);
            LOGGER.info(String.format("[INVENTORY DELETED] Removed SKU %s (ID %d)", item.getSku(), id));
        }
    }

    @Override
    @LogisticsAudited(actionCode = "INVENTORY_REORDER_TRIGGERED", targetEntity = "InventoryItem")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void triggerAutoReplenishment(InventoryItem item) throws GlobalTradeException {
        InventoryItem managedItem = em.find(InventoryItem.class, item.getId());
        if (managedItem != null) {
            int reorderQty = managedItem.getReorderQuantity();
            managedItem.setStockLevel(managedItem.getStockLevel() + reorderQty);
            em.merge(managedItem);
            LOGGER.info(String.format("[EJB REQUIRES_NEW AUTO-REPLENISHMENT] Reordered %d units for SKU %s. New Stock: %d", reorderQty, managedItem.getSku(), managedItem.getStockLevel()));
        }
    }
}
