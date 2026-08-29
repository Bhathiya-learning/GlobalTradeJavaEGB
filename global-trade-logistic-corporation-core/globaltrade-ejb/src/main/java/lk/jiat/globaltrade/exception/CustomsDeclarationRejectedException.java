package lk.jiat.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CustomsDeclarationRejectedException extends GlobalTradeException {

    private static final long serialVersionUID = 1L;

    private final String declarationNumber;
    private final String reason;

    public CustomsDeclarationRejectedException(String declarationNumber, String reason) {
        super(String.format("Customs Declaration %s rejected: %s", declarationNumber, reason));
        this.declarationNumber = declarationNumber;
        this.reason = reason;
    }

    public String getDeclarationNumber() { return declarationNumber; }
    public String getReason() { return reason; }
}
