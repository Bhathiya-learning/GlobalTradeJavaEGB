package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_evaluations")
public class VendorEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "evaluation_date", insertable = false, updatable = false)
    private LocalDateTime evaluationDate;

    @Column(name = "delivery_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal deliveryScore;

    @Column(name = "quality_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "compliance_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal complianceScore;

    @Column(name = "overall_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal overallRating;

    @Column(name = "evaluation_period", nullable = false, length = 30)
    private String evaluationPeriod;

    public VendorEvaluation() {}

    public VendorEvaluation(Long id, Vendor vendor, BigDecimal deliveryScore, BigDecimal qualityScore, BigDecimal complianceScore, BigDecimal overallRating, String evaluationPeriod) {
        this.id = id;
        this.vendor = vendor;
        this.deliveryScore = deliveryScore;
        this.qualityScore = qualityScore;
        this.complianceScore = complianceScore;
        this.overallRating = overallRating;
        this.evaluationPeriod = evaluationPeriod;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public LocalDateTime getEvaluationDate() { return evaluationDate; }
    public void setEvaluationDate(LocalDateTime evaluationDate) { this.evaluationDate = evaluationDate; }

    public BigDecimal getDeliveryScore() { return deliveryScore; }
    public void setDeliveryScore(BigDecimal deliveryScore) { this.deliveryScore = deliveryScore; }

    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }

    public BigDecimal getComplianceScore() { return complianceScore; }
    public void setComplianceScore(BigDecimal complianceScore) { this.complianceScore = complianceScore; }

    public BigDecimal getOverallRating() { return overallRating; }
    public void setOverallRating(BigDecimal overallRating) { this.overallRating = overallRating; }

    public String getEvaluationPeriod() { return evaluationPeriod; }
    public void setEvaluationPeriod(String evaluationPeriod) { this.evaluationPeriod = evaluationPeriod; }
}
