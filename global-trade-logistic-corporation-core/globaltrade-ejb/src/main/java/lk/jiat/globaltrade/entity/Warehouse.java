package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "warehouses")
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_code", nullable = false, unique = true, length = 50)
    private String warehouseCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(name = "total_capacity_sqm", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCapacitySqm;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Warehouse() {}

    public Warehouse(Long id, String warehouseCode, String name, Address address, BigDecimal totalCapacitySqm, Boolean isActive) {
        this.id = id;
        this.warehouseCode = warehouseCode;
        this.name = name;
        this.address = address;
        this.totalCapacitySqm = totalCapacitySqm;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public BigDecimal getTotalCapacitySqm() { return totalCapacitySqm; }
    public void setTotalCapacitySqm(BigDecimal totalCapacitySqm) { this.totalCapacitySqm = totalCapacitySqm; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
