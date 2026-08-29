package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import lk.jiat.globaltrade.entity.Vendor;
import lk.jiat.globaltrade.entity.VendorEvaluation;

import java.math.BigDecimal;
import java.util.List;

@Local
public interface VendorServiceLocal {
    List<Vendor> getAllVendors();
    Vendor getVendorById(Long id);
    Vendor getVendorByCode(String vendorCode);
    Vendor createVendor(Vendor vendor, Long countryId, String complianceStatusCode);
    Vendor updateVendor(Long vendorId, String companyName, String contactEmail, String contactPhone, Long countryId, String complianceStatusCode);
    void deleteVendor(Long vendorId);
    void updateVendorCompliance(Long vendorId, String statusCode);
    void recordVendorEvaluation(Long vendorId, BigDecimal deliveryScore, BigDecimal qualityScore, BigDecimal complianceScore, String period);
    List<VendorEvaluation> getVendorEvaluations(Long vendorId);
}
