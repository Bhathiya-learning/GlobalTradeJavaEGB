package lk.jiat.globaltrade.service;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import lk.jiat.globaltrade.entity.InventoryItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Stateful
public class GlobalTradeStatefulCartBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(GlobalTradeStatefulCartBean.class.getName());

    private final List<InventoryItem> draftItems = new ArrayList<>();
    private final List<Integer> draftQuantities = new ArrayList<>();

    public void addItem(InventoryItem item, int quantity) {
        draftItems.add(item);
        draftQuantities.add(quantity);
        LOGGER.info(String.format("[STATEFUL EJB CART] Added item %s (Qty: %d)", item.getSku(), quantity));
    }

    public List<InventoryItem> getDraftItems() {
        return draftItems;
    }

    public List<Integer> getDraftQuantities() {
        return draftQuantities;
    }

    public void clear() {
        draftItems.clear();
        draftQuantities.clear();
    }

    @Remove
    public void checkout() {
        LOGGER.info("[STATEFUL EJB REMOVE] Checkout session completed. Cleaning up stateful bean.");
        clear();
    }
}
