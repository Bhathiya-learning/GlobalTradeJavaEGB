package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import java.util.Map;

@Local
public interface PerformanceMetricsServiceLocal {
    Map<String, Object> getSystemMetrics();
}
