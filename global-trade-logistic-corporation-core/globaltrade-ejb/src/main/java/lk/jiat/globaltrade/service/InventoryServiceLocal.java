package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import lk.jiat.globaltrade.entity.InventoryItem;
import lk.jiat.globaltrade.exception.GlobalTradeException;

import java.util.List;

@Local
public interface InventoryServiceLocal {
    List<InventoryItem> getAllInventoryItems();
    List<InventoryItem> getLowStockItems();
    InventoryItem getItemBySku(String sku);
    InventoryItem getInventoryItemById(Long id);
    InventoryItem createInventoryItem(String sku, String name, String description, java.math.BigDecimal unitPrice, int stockLevel, int minStockLevel, int reorderQuantity, Long categoryId, Long vendorId, Long warehouseId) throws GlobalTradeException;
    InventoryItem updateInventoryItem(Long id, String sku, String name, String description, java.math.BigDecimal unitPrice, int stockLevel, int minStockLevel, int reorderQuantity, Long categoryId, Long vendorId, Long warehouseId) throws GlobalTradeException;
    void adjustStockLevel(Long itemId, int delta);
    void deleteInventoryItem(Long id) throws GlobalTradeException;
    void triggerAutoReplenishment(InventoryItem item) throws GlobalTradeException;
}
