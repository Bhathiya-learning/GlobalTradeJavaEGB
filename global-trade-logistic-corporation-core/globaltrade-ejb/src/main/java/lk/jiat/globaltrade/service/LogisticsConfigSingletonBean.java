package lk.jiat.globaltrade.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class LogisticsConfigSingletonBean {

    private static final Logger LOGGER = Logger.getLogger(LogisticsConfigSingletonBean.class.getName());

    private final Map<String, String> configMap = new HashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.info("[SINGLETON STARTUP EJB] Initializing GlobalTrade Enterprise Logistics System Configurations...");
        configMap.put("COMPANY_NAME", "GlobalTrade Logistics Corporation");
        configMap.put("DEFAULT_CURRENCY", "USD");
        configMap.put("MAX_SHIPMENT_ITEMS", "50");
        configMap.put("CARRIER_POLL_INTERVAL_MIN", "15");
        configMap.put("SLA_THRESHOLD_MS", "500");
    }

    @Lock(LockType.READ)
    public String getConfig(String key) {
        return configMap.get(key);
    }

    @Lock(LockType.WRITE)
    public void setConfig(String key, String value) {
        configMap.put(key, value);
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("[SINGLETON EJB SHUTDOWN] Releasing global system configurations.");
    }
}
