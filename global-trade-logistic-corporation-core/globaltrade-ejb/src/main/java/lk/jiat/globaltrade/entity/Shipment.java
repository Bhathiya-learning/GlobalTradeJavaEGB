package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 50)
    private String trackingNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "origin_warehouse_id", nullable = false)
    private Warehouse originWarehouse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_address_id", nullable = false)
    private Address destinationAddress;

    @ManyToOne(optional = false)
    @JoinColumn(name = "carrier_id", nullable = false)
    private Carrier carrier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private ShipmentStatus status;

    @Column(name = "dispatch_date")
    private LocalDateTime dispatchDate;

    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @Column(name = "actual_delivery")
    private LocalDateTime actualDelivery;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ShipmentItem> shipmentItems = new ArrayList<>();

    public Shipment() {}

    public Shipment(Long id, String trackingNumber, Warehouse originWarehouse, Address destinationAddress, Carrier carrier, ShipmentStatus status, LocalDateTime dispatchDate, LocalDateTime estimatedDelivery, LocalDateTime actualDelivery, User createdByUser) {
        this.id = id;
        this.trackingNumber = trackingNumber;
        this.originWarehouse = originWarehouse;
        this.destinationAddress = destinationAddress;
        this.carrier = carrier;
        this.status = status;
        this.dispatchDate = dispatchDate;
        this.estimatedDelivery = estimatedDelivery;
        this.actualDelivery = actualDelivery;
        this.createdByUser = createdByUser;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public Warehouse getOriginWarehouse() { return originWarehouse; }
    public void setOriginWarehouse(Warehouse originWarehouse) { this.originWarehouse = originWarehouse; }

    public Address getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(Address destinationAddress) { this.destinationAddress = destinationAddress; }

    public Carrier getCarrier() { return carrier; }
    public void setCarrier(Carrier carrier) { this.carrier = carrier; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public LocalDateTime getDispatchDate() { return dispatchDate; }
    public void setDispatchDate(LocalDateTime dispatchDate) { this.dispatchDate = dispatchDate; }

    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public LocalDateTime getActualDelivery() { return actualDelivery; }
    public void setActualDelivery(LocalDateTime actualDelivery) { this.actualDelivery = actualDelivery; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public User getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ShipmentItem> getShipmentItems() { return shipmentItems; }
    public void setShipmentItems(List<ShipmentItem> shipmentItems) { this.shipmentItems = shipmentItems; }

    public void addShipmentItem(ShipmentItem item) {
        shipmentItems.add(item);
        item.setShipment(this);
    }
}
