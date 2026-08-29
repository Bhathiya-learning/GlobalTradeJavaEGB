package lk.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class UnauthorizedLogisticsAccessException extends GlobalTradeException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedLogisticsAccessException(String user, String requiredRole) {
        super(String.format("User %s lacks required role [%s] for this logistics operation.", user, requiredRole));
    }
}
