package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;

@Local
public interface AuditLogServiceLocal {
    void recordAudit(String userEmail, String actionCode, String targetEntity, Long entityId, long executionTimeMs, boolean success, String ipAddress, String details);
}
