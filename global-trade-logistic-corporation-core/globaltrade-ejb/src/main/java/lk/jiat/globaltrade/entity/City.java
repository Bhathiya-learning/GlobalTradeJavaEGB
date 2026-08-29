package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "cities")
public class City implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    public City() {}

    public City(Long id, Country country, String name, String postalCode) {
        this.id = id;
        this.country = country;
        this.name = name;
        this.postalCode = postalCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
}
