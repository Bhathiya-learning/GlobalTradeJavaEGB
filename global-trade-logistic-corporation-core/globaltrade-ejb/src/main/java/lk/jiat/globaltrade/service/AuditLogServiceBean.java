package lk.jiat.globaltrade.service;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.jiat.globaltrade.entity.AuditAction;
import lk.jiat.globaltrade.entity.LogisticsAuditTrail;
import lk.jiat.globaltrade.entity.User;

import java.util.List;
import java.util.logging.Logger;

@Stateless
public class AuditLogServiceBean implements AuditLogServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(AuditLogServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void recordAudit(String userEmail, String actionCode, String targetEntity, Long entityId, long executionTimeMs, boolean success, String ipAddress, String details) {
        try {
            User user = null;
            if (userEmail != null && !userEmail.equals("SYSTEM")) {
                List<User> users = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                                    .setParameter("email", userEmail)
                                    .getResultList();
                if (!users.isEmpty()) {
                    user = users.get(0);
                }
            }

            List<AuditAction> actions = em.createQuery("SELECT a FROM AuditAction a WHERE a.code = :code", AuditAction.class)
                                          .setParameter("code", actionCode)
                                          .getResultList();
            AuditAction action;
            if (actions.isEmpty()) {
                action = new AuditAction(null, actionCode, "Dynamic audit action: " + actionCode);
                em.persist(action);
            } else {
                action = actions.get(0);
            }

            LogisticsAuditTrail audit = new LogisticsAuditTrail(
                null, user, action, targetEntity, entityId, executionTimeMs, success, ipAddress, details
            );

            em.persist(audit);
            LOGGER.info("[AUDIT PERSISTED] Action: " + actionCode + " | Execution Time: " + executionTimeMs + "ms");
        } catch (Exception e) {
            LOGGER.severe("Audit record failed: " + e.getMessage());
        }
    }
}
