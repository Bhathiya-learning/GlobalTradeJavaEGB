package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import lk.jiat.globaltrade.entity.CustomsDeclaration;
import lk.jiat.globaltrade.exception.CustomsDeclarationRejectedException;
import lk.jiat.globaltrade.exception.GlobalTradeException;

import java.math.BigDecimal;
import java.util.List;

@Local
public interface CustomsClearanceServiceLocal {
    List<CustomsDeclaration> getAllDeclarations();
    CustomsDeclaration getDeclarationByNumber(String declarationNumber);
    CustomsDeclaration approveDeclaration(Long declarationId, Long inspectorUserId, BigDecimal calculatedDuty, String notes) throws GlobalTradeException, CustomsDeclarationRejectedException;
    CustomsDeclaration rejectDeclaration(Long declarationId, Long inspectorUserId, String reason) throws GlobalTradeException;
}
