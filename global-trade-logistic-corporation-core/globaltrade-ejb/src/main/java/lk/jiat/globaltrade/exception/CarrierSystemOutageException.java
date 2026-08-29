package lk.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class CarrierSystemOutageException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String carrierCode;

    public CarrierSystemOutageException(String carrierCode, String message) {
        super(String.format("Carrier %s system outage: %s", carrierCode, message));
        this.carrierCode = carrierCode;
    }

    public String getCarrierCode() { return carrierCode; }
}
