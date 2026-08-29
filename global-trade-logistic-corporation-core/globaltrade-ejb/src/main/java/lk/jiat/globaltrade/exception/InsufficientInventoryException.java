package lk.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InsufficientInventoryException extends GlobalTradeException {

    private static final long serialVersionUID = 1L;

    private final String sku;
    private final int requestedQuantity;
    private final int availableStock;

    public InsufficientInventoryException(String sku, int requestedQuantity, int availableStock) {
        super(String.format("Insufficient inventory for SKU %s: Requested %d, Available %d", sku, requestedQuantity, availableStock));
        this.sku = sku;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }

    public String getSku() { return sku; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getAvailableStock() { return availableStock; }
}
