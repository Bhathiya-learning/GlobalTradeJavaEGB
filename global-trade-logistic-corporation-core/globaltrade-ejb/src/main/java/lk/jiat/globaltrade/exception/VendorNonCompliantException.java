package lk.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class VendorNonCompliantException extends GlobalTradeException {

    private static final long serialVersionUID = 1L;

    private final String vendorCode;
    private final String status;

    public VendorNonCompliantException(String vendorCode, String status) {
        super(String.format("Vendor %s is non-compliant (Status: %s). Operation aborted.", vendorCode, status));
        this.vendorCode = vendorCode;
        this.status = status;
    }

    public String getVendorCode() { return vendorCode; }
    public String getStatus() { return status; }
}
