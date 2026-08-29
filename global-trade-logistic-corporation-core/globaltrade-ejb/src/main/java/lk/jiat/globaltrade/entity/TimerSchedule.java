package lk.jiat.globaltrade.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "timer_schedules")
public class TimerSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timer_name", nullable = false, unique = true, length = 100)
    private String timerName;

    @Column(name = "schedule_expression", nullable = false, length = 100)
    private String scheduleExpression;

    @Column(name = "last_run")
    private LocalDateTime lastRun;

    @Column(name = "next_run")
    private LocalDateTime nextRun;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private TimerStatus status;

    @Column(name = "execution_count", nullable = false)
    private Long executionCount = 0L;

    public TimerSchedule() {}

    public TimerSchedule(Long id, String timerName, String scheduleExpression, LocalDateTime lastRun, LocalDateTime nextRun, TimerStatus status, Long executionCount) {
        this.id = id;
        this.timerName = timerName;
        this.scheduleExpression = scheduleExpression;
        this.lastRun = lastRun;
        this.nextRun = nextRun;
        this.status = status;
        this.executionCount = executionCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTimerName() { return timerName; }
    public void setTimerName(String timerName) { this.timerName = timerName; }

    public String getScheduleExpression() { return scheduleExpression; }
    public void setScheduleExpression(String scheduleExpression) { this.scheduleExpression = scheduleExpression; }

    public LocalDateTime getLastRun() { return lastRun; }
    public void setLastRun(LocalDateTime lastRun) { this.lastRun = lastRun; }

    public LocalDateTime getNextRun() { return nextRun; }
    public void setNextRun(LocalDateTime nextRun) { this.nextRun = nextRun; }

    public TimerStatus getStatus() { return status; }
    public void setStatus(TimerStatus status) { this.status = status; }

    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }
}
