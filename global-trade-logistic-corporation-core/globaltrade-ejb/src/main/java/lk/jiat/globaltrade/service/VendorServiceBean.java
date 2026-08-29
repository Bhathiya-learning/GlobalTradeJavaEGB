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
import lk.jiat.globaltrade.annotation.VendorValidated;
import lk.jiat.globaltrade.entity.Vendor;
import lk.jiat.globaltrade.entity.VendorComplianceStatus;
import lk.jiat.globaltrade.entity.VendorEvaluation;
import lk.jiat.globaltrade.interceptor.LogisticsAuditInterceptor;
import lk.jiat.globaltrade.interceptor.VendorValidationInterceptor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@DeclareRoles({"ADMIN", "LOGISTICS_COORDINATOR", "VENDOR_REPRESENTATIVE"})
@Interceptors({LogisticsAuditInterceptor.class, VendorValidationInterceptor.class})
public class VendorServiceBean implements VendorServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(VendorServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> getAllVendors() {
        return em.createQuery("SELECT v FROM Vendor v ORDER BY v.companyName ASC", Vendor.class).getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Vendor getVendorById(Long id) {
        return em.find(Vendor.class, id);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Vendor getVendorByCode(String vendorCode) {
        List<Vendor> list = em.createQuery("SELECT v FROM Vendor v WHERE v.vendorCode = :vc", Vendor.class)
                             .setParameter("vc", vendorCode)
                             .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Vendor createVendor(Vendor vendor, Long countryId, String complianceStatusCode) {
        lk.jiat.globaltrade.entity.Country country = em.find(lk.jiat.globaltrade.entity.Country.class, countryId != null ? countryId : 1L);
        VendorComplianceStatus status = em.createQuery("SELECT s FROM VendorComplianceStatus s WHERE s.code = :code", VendorComplianceStatus.class)
                                          .setParameter("code", complianceStatusCode != null ? complianceStatusCode : "COMPLIANT")
                                          .getSingleResult();

        if (vendor.getVendorCode() == null || vendor.getVendorCode().trim().isEmpty()) {
            vendor.setVendorCode("VND-" + (System.currentTimeMillis() % 100000));
        }

        vendor.setCountry(country);
        vendor.setComplianceStatus(status);
        if (vendor.getRating() == null) {
            vendor.setRating(new java.math.BigDecimal("5.00"));
        }

        em.persist(vendor);
        LOGGER.info("[VENDOR CREATED] Registered vendor " + vendor.getVendorCode() + " - " + vendor.getCompanyName());
        return vendor;
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Vendor updateVendor(Long vendorId, String companyName, String contactEmail, String contactPhone, Long countryId, String complianceStatusCode) {
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor != null) {
            if (companyName != null && !companyName.trim().isEmpty()) vendor.setCompanyName(companyName.trim());
            if (contactEmail != null && !contactEmail.trim().isEmpty()) vendor.setContactEmail(contactEmail.trim());
            if (contactPhone != null) vendor.setContactPhone(contactPhone.trim());
            if (countryId != null) {
                lk.jiat.globaltrade.entity.Country c = em.find(lk.jiat.globaltrade.entity.Country.class, countryId);
                if (c != null) vendor.setCountry(c);
            }
            if (complianceStatusCode != null && !complianceStatusCode.trim().isEmpty()) {
                VendorComplianceStatus status = em.createQuery("SELECT s FROM VendorComplianceStatus s WHERE s.code = :code", VendorComplianceStatus.class)
                                                  .setParameter("code", complianceStatusCode.trim())
                                                  .getSingleResult();
                vendor.setComplianceStatus(status);
            }
            em.merge(vendor);
            LOGGER.info("[VENDOR UPDATED] Updated vendor details for " + vendor.getVendorCode());
        }
        return vendor;
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteVendor(Long vendorId) {
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor != null) {
            em.createQuery("UPDATE User u SET u.vendor = NULL WHERE u.vendor.id = :vid")
              .setParameter("vid", vendorId)
              .executeUpdate();
            em.remove(vendor);
            LOGGER.info("[VENDOR DELETED] Removed vendor ID: " + vendorId);
        }
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @VendorValidated
    @LogisticsAudited(actionCode = "VENDOR_COMPLIANCE_UPDATE", targetEntity = "Vendor")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateVendorCompliance(Long vendorId, String statusCode) {
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor != null) {
            VendorComplianceStatus status = em.createQuery("SELECT s FROM VendorComplianceStatus s WHERE s.code = :code", VendorComplianceStatus.class)
                                              .setParameter("code", statusCode)
                                              .getSingleResult();
            vendor.setComplianceStatus(status);
            em.merge(vendor);
            LOGGER.info("[VENDOR COMPLIANCE UPDATED] Vendor " + vendor.getVendorCode() + " set to " + statusCode);
        }
    }

    @Override
    @LogisticsAudited(actionCode = "VENDOR_EVALUATION_RECORDED", targetEntity = "VendorEvaluation")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void recordVendorEvaluation(Long vendorId, BigDecimal deliveryScore, BigDecimal qualityScore, BigDecimal complianceScore, String period) {
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor != null) {
            BigDecimal totalScore = deliveryScore.add(qualityScore).add(complianceScore);
            BigDecimal avgScore = totalScore.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
            BigDecimal rating = avgScore.divide(new BigDecimal("20"), 2, RoundingMode.HALF_UP); // convert 100% to 5.0 rating

            VendorEvaluation eval = new VendorEvaluation(null, vendor, deliveryScore, qualityScore, complianceScore, rating, period);
            em.persist(eval);

            vendor.setRating(rating);
            em.merge(vendor);

            LOGGER.info(String.format("[VENDOR EVALUATION CREATED] Vendor %s evaluated for %s -> Rating: %s", vendor.getVendorCode(), period, rating));
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<VendorEvaluation> getVendorEvaluations(Long vendorId) {
        return em.createQuery("SELECT e FROM VendorEvaluation e WHERE e.vendor.id = :vid ORDER BY e.id DESC", VendorEvaluation.class)
                 .setParameter("vid", vendorId)
                 .getResultList();
    }
}
