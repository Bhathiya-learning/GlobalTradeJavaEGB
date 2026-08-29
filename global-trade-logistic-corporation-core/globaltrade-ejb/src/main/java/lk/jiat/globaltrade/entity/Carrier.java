package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "carriers")
public class Carrier implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carrier_code", nullable = false, unique = true, length = 30)
    private String carrierCode;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Carrier() {}

    public Carrier(Long id, String carrierCode, String companyName, String contactEmail, String contactPhone, Boolean isActive) {
        this.id = id;
        this.carrierCode = carrierCode;
        this.companyName = companyName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCarrierCode() { return carrierCode; }
    public void setCarrierCode(String carrierCode) { this.carrierCode = carrierCode; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
