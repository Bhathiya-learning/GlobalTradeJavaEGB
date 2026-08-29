package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customs_declarations")
public class CustomsDeclaration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "declaration_number", nullable = false, unique = true, length = 50)
    private String declarationNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customs_status_id", nullable = false)
    private CustomsStatus customsStatus;

    @Column(name = "declaration_date", insertable = false, updatable = false)
    private LocalDateTime declarationDate;

    @Column(name = "duty_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dutyAmount = BigDecimal.ZERO;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hs_tariff_code_id", nullable = false)
    private HsTariffCode hsTariffCode;

    @ManyToOne
    @JoinColumn(name = "inspected_by_user_id")
    private User inspectedByUser;

    @Column(name = "compliance_notes", columnDefinition = "TEXT")
    private String complianceNotes;

    public CustomsDeclaration() {}

    public CustomsDeclaration(Long id, String declarationNumber, Shipment shipment, CustomsStatus customsStatus, BigDecimal dutyAmount, HsTariffCode hsTariffCode, User inspectedByUser, String complianceNotes) {
        this.id = id;
        this.declarationNumber = declarationNumber;
        this.shipment = shipment;
        this.customsStatus = customsStatus;
        this.dutyAmount = dutyAmount;
        this.hsTariffCode = hsTariffCode;
        this.inspectedByUser = inspectedByUser;
        this.complianceNotes = complianceNotes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeclarationNumber() { return declarationNumber; }
    public void setDeclarationNumber(String declarationNumber) { this.declarationNumber = declarationNumber; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public CustomsStatus getCustomsStatus() { return customsStatus; }
    public void setCustomsStatus(CustomsStatus customsStatus) { this.customsStatus = customsStatus; }

    public LocalDateTime getDeclarationDate() { return declarationDate; }
    public void setDeclarationDate(LocalDateTime declarationDate) { this.declarationDate = declarationDate; }

    public BigDecimal getDutyAmount() { return dutyAmount; }
    public void setDutyAmount(BigDecimal dutyAmount) { this.dutyAmount = dutyAmount; }

    public HsTariffCode getHsTariffCode() { return hsTariffCode; }
    public void setHsTariffCode(HsTariffCode hsTariffCode) { this.hsTariffCode = hsTariffCode; }

    public User getInspectedByUser() { return inspectedByUser; }
    public void setInspectedByUser(User inspectedByUser) { this.inspectedByUser = inspectedByUser; }

    public String getComplianceNotes() { return complianceNotes; }
    public void setComplianceNotes(String complianceNotes) { this.complianceNotes = complianceNotes; }
}
