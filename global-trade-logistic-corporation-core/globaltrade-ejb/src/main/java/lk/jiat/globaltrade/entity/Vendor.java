package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendors")
public class Vendor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_code", nullable = false, unique = true, length = 50)
    private String vendorCode;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = new BigDecimal("5.00");

    @ManyToOne(optional = false)
    @JoinColumn(name = "compliance_status_id", nullable = false)
    private VendorComplianceStatus complianceStatus;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Vendor() {}

    public Vendor(Long id, String vendorCode, String companyName, Country country, String contactEmail, String contactPhone, BigDecimal rating, VendorComplianceStatus complianceStatus) {
        this.id = id;
        this.vendorCode = vendorCode;
        this.companyName = companyName;
        this.country = country;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.rating = rating;
        this.complianceStatus = complianceStatus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public VendorComplianceStatus getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(VendorComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
