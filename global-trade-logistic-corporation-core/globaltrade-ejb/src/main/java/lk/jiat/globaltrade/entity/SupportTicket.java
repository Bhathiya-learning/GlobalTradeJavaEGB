package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_tickets")
public class SupportTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "shipment_tracking", length = 100)
    private String shipmentTracking;

    @Column(nullable = false, length = 30)
    private String status; // 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sentAt ASC")
    private List<SupportMessage> messages = new ArrayList<>();

    public SupportTicket() {}

    public SupportTicket(Long id, String ticketNumber, User user, String subject, String category, String shipmentTracking, String status) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.user = user;
        this.subject = subject;
        this.category = category;
        this.shipmentTracking = shipmentTracking;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getShipmentTracking() {
        return shipmentTracking;
    }

    public void setShipmentTracking(String shipmentTracking) {
        this.shipmentTracking = shipmentTracking;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SupportMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<SupportMessage> messages) {
        this.messages = messages;
    }
}
