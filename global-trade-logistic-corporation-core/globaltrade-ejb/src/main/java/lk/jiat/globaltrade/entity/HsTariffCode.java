package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "hs_tariff_codes")
public class HsTariffCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hs_code", nullable = false, unique = true, length = 50)
    private String hsCode;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "default_duty_rate_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultDutyRatePct;

    public HsTariffCode() {}

    public HsTariffCode(Long id, String hsCode, String description, BigDecimal defaultDutyRatePct) {
        this.id = id;
        this.hsCode = hsCode;
        this.description = description;
        this.defaultDutyRatePct = defaultDutyRatePct;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHsCode() { return hsCode; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDefaultDutyRatePct() { return defaultDutyRatePct; }
    public void setDefaultDutyRatePct(BigDecimal defaultDutyRatePct) { this.defaultDutyRatePct = defaultDutyRatePct; }
}
