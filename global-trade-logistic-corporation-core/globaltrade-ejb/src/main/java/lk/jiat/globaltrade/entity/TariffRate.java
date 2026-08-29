package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "tariff_rates")
public class TariffRate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_key", nullable = false, unique = true, length = 50)
    private String rateKey;

    @Column(name = "rate_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal rateValue;

    @Column(name = "description", length = 255)
    private String description;

    public TariffRate() {}

    public TariffRate(String rateKey, BigDecimal rateValue, String description) {
        this.rateKey = rateKey;
        this.rateValue = rateValue;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRateKey() {
        return rateKey;
    }

    public void setRateKey(String rateKey) {
        this.rateKey = rateKey;
    }

    public BigDecimal getRateValue() {
        return rateValue;
    }

    public void setRateValue(BigDecimal rateValue) {
        this.rateValue = rateValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
