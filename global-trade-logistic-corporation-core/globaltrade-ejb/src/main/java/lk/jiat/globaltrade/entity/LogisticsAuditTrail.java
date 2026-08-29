package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_audit_trail")
public class LogisticsAuditTrail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "action_id", nullable = false)
    private AuditAction action;

    @Column(name = "target_entity", nullable = false, length = 50)
    private String targetEntity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs = 0L;

    @Column(name = "compliance_flag", nullable = false)
    private Boolean complianceFlag = true;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "timestamp", insertable = false, updatable = false)
    private LocalDateTime timestamp;

    public LogisticsAuditTrail() {}

    public LogisticsAuditTrail(Long id, User user, AuditAction action, String targetEntity, Long entityId, Long executionTimeMs, Boolean complianceFlag, String ipAddress, String details) {
        this.id = id;
        this.user = user;
        this.action = action;
        this.targetEntity = targetEntity;
        this.entityId = entityId;
        this.executionTimeMs = executionTimeMs;
        this.complianceFlag = complianceFlag;
        this.ipAddress = ipAddress;
        this.details = details;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public Boolean getComplianceFlag() { return complianceFlag; }
    public void setComplianceFlag(Boolean complianceFlag) { this.complianceFlag = complianceFlag; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
