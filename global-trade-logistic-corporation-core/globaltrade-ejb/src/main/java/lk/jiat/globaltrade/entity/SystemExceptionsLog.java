package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_exceptions_log")
public class SystemExceptionsLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exception_class", nullable = false, length = 255)
    private String exceptionClass;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @ManyToOne(optional = false)
    @JoinColumn(name = "failure_scenario_id", nullable = false)
    private FailureScenario failureScenario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "severity_id", nullable = false)
    private SeverityLevel severity;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "logged_at", insertable = false, updatable = false)
    private LocalDateTime loggedAt;

    public SystemExceptionsLog() {}

    public SystemExceptionsLog(Long id, String exceptionClass, String message, FailureScenario failureScenario, SeverityLevel severity, String stackTrace) {
        this.id = id;
        this.exceptionClass = exceptionClass;
        this.message = message;
        this.failureScenario = failureScenario;
        this.severity = severity;
        this.stackTrace = stackTrace;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExceptionClass() { return exceptionClass; }
    public void setExceptionClass(String exceptionClass) { this.exceptionClass = exceptionClass; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public FailureScenario getFailureScenario() { return failureScenario; }
    public void setFailureScenario(FailureScenario failureScenario) { this.failureScenario = failureScenario; }

    public SeverityLevel getSeverity() { return severity; }
    public void setSeverity(SeverityLevel severity) { this.severity = severity; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
