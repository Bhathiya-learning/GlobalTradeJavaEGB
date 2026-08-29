package lk.jiat.globaltrade.timer;

import jakarta.annotation.Resource;

import jakarta.ejb.*;
import lk.jiat.globaltrade.entity.InventoryItem;
import lk.jiat.globaltrade.service.InventoryServiceLocal;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class InventoryReplenishmentTimerBean {

    private static final Logger LOGGER = Logger.getLogger(InventoryReplenishmentTimerBean.class.getName());

    @Resource
    private TimerService timerService;

    @EJB
    private InventoryServiceLocal inventoryService;

    public void createProgrammaticTimer(long intervalMs) {
        // Cancel existing timer with same info
        Collection<Timer> timers = timerService.getTimers();
        for (Timer t : timers) {
            if ("PROGRAMMATIC_INVENTORY_REPLENISHMENT".equals(t.getInfo())) {
                t.cancel();
            }
        }
        timerService.createIntervalTimer(1000L, intervalMs, new TimerConfig("PROGRAMMATIC_INVENTORY_REPLENISHMENT", true));
        LOGGER.info("[EJB TIMER PROGRAMMATIC] Created persistent interval timer for auto-replenishment every " + intervalMs + " ms");
    }

    @Timeout
    public void onTimeout(Timer timer) {
        LOGGER.info("[EJB TIMER TIMEOUT] Executing Programmatic Inventory Replenishment Check...");
        try {
            List<InventoryItem> lowStockItems = inventoryService.getLowStockItems();
            for (InventoryItem item : lowStockItems) {
                LOGGER.warning(String.format("[AUTO-REPLENISHMENT TRIGGERED] Item SKU %s (Stock: %d <= Min: %d)", item.getSku(), item.getStockLevel(), item.getMinStockLevel()));
                inventoryService.triggerAutoReplenishment(item);
            }
        } catch (Exception e) {
            LOGGER.severe("Auto-replenishment timer execution error: " + e.getMessage());
        }
    }
}
