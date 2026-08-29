package lk.jiat.globaltrade.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

import java.util.logging.Logger;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN) // EJB Bean-Managed Transaction (BMT)
public class BatchLogisticsServiceBean {

    private static final Logger LOGGER = Logger.getLogger(BatchLogisticsServiceBean.class.getName());

    @Resource
    private UserTransaction userTransaction;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    public void processBulkAuditCleanup() {
        try {
            LOGGER.info("[EJB BMT START] Beginning explicit UserTransaction for batch audit maintenance...");
            userTransaction.begin();

            int updated = em.createQuery("UPDATE LogisticsAuditTrail a SET a.complianceFlag = TRUE WHERE a.executionTimeMs < 50").executeUpdate();

            userTransaction.commit();
            LOGGER.info(String.format("[EJB BMT COMMIT SUCCESS] Committed batch audit update for %d records", updated));
        } catch (Exception e) {
            LOGGER.severe("[EJB BMT ROLLBACK] Batch operation failed: " + e.getMessage());
            try {
                userTransaction.rollback();
            } catch (Exception rbEx) {
                LOGGER.severe("Rollback failure: " + rbEx.getMessage());
            }
        }
    }
}
