package lk.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class GlobalTradeException extends Exception {

    private static final long serialVersionUID = 1L;

    public GlobalTradeException(String message) {
        super(message);
    }

    public GlobalTradeException(String message, Throwable cause) {
        super(message, cause);
    }
}
