package lk.jiat.globaltrade.service;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.jiat.globaltrade.annotation.LogisticsAudited;
import lk.jiat.globaltrade.annotation.TradeCompliant;
import lk.jiat.globaltrade.entity.CustomsDeclaration;
import lk.jiat.globaltrade.entity.CustomsStatus;
import lk.jiat.globaltrade.entity.User;
import lk.jiat.globaltrade.exception.CustomsDeclarationRejectedException;
import lk.jiat.globaltrade.exception.GlobalTradeException;
import lk.jiat.globaltrade.interceptor.LogisticsAuditInterceptor;
import lk.jiat.globaltrade.interceptor.TradeComplianceInterceptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@DeclareRoles({"ADMIN", "CUSTOMS_OFFICIAL", "LOGISTICS_COORDINATOR"})
@Interceptors({LogisticsAuditInterceptor.class, TradeComplianceInterceptor.class})
public class CustomsClearanceServiceBean implements CustomsClearanceServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(CustomsClearanceServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDeclaration> getAllDeclarations() {
        return em.createQuery("SELECT d FROM CustomsDeclaration d ORDER BY d.id DESC", CustomsDeclaration.class).getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CustomsDeclaration getDeclarationByNumber(String declarationNumber) {
        List<CustomsDeclaration> list = em.createQuery("SELECT d FROM CustomsDeclaration d WHERE d.declarationNumber = :dn", CustomsDeclaration.class)
                                         .setParameter("dn", declarationNumber)
                                         .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @RolesAllowed({"ADMIN", "CUSTOMS_OFFICIAL"})
    @LogisticsAudited(actionCode = "CUSTOMS_APPROVAL", targetEntity = "CustomsDeclaration")
    @TradeCompliant
    @TransactionAttribute(TransactionAttributeType.MANDATORY) // EJB MANDATORY: Client must possess an active transaction!
    public CustomsDeclaration approveDeclaration(Long declarationId, Long inspectorUserId, BigDecimal calculatedDuty, String notes) throws GlobalTradeException, CustomsDeclarationRejectedException {
        CustomsDeclaration decl = em.find(CustomsDeclaration.class, declarationId);
        if (decl == null) {
            throw new GlobalTradeException("Customs declaration not found for ID: " + declarationId);
        }

        User inspector = (inspectorUserId != null) ? em.find(User.class, inspectorUserId) : null;
        CustomsStatus approvedStatus = em.createQuery("SELECT s FROM CustomsStatus s WHERE s.code = 'APPROVED'", CustomsStatus.class).getSingleResult();

        decl.setCustomsStatus(approvedStatus);
        decl.setDutyAmount(calculatedDuty);
        decl.setInspectedByUser(inspector);
        decl.setComplianceNotes(notes);

        em.merge(decl);
        LOGGER.info("[EJB MANDATORY CUSTOMS APPROVED] Declaration " + decl.getDeclarationNumber() + " cleared with duty $" + calculatedDuty);
        return decl;
    }

    @Override
    @RolesAllowed({"ADMIN", "CUSTOMS_OFFICIAL"})
    @LogisticsAudited(actionCode = "CUSTOMS_REJECT", targetEntity = "CustomsDeclaration")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CustomsDeclaration rejectDeclaration(Long declarationId, Long inspectorUserId, String reason) throws GlobalTradeException {
        CustomsDeclaration decl = em.find(CustomsDeclaration.class, declarationId);
        if (decl == null) {
            throw new GlobalTradeException("Customs declaration not found for ID: " + declarationId);
        }

        User inspector = (inspectorUserId != null) ? em.find(User.class, inspectorUserId) : null;
        CustomsStatus rejectedStatus = em.createQuery("SELECT s FROM CustomsStatus s WHERE s.code = 'REJECTED'", CustomsStatus.class).getSingleResult();

        decl.setCustomsStatus(rejectedStatus);
        decl.setInspectedByUser(inspector);
        decl.setComplianceNotes("REJECTED: " + reason);

        em.merge(decl);
        LOGGER.warning("[CUSTOMS REJECTED] Declaration " + decl.getDeclarationNumber() + ": " + reason);
        return decl;
    }
}
