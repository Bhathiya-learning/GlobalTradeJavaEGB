package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "countries")
public class Country implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso_code_2", nullable = false, unique = true, length = 2)
    private String isoCode2;

    @Column(name = "iso_code_3", nullable = false, unique = true, length = 3)
    private String isoCode3;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    public Country() {}

    public Country(Long id, String isoCode2, String isoCode3, String name) {
        this.id = id;
        this.isoCode2 = isoCode2;
        this.isoCode3 = isoCode3;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIsoCode2() { return isoCode2; }
    public void setIsoCode2(String isoCode2) { this.isoCode2 = isoCode2; }

    public String getIsoCode3() { return isoCode3; }
    public void setIsoCode3(String isoCode3) { this.isoCode3 = isoCode3; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
