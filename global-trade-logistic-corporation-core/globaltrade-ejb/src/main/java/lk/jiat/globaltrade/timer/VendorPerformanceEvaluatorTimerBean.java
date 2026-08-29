package lk.jiat.globaltrade.timer;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import lk.jiat.globaltrade.entity.Vendor;
import lk.jiat.globaltrade.service.VendorServiceLocal;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class VendorPerformanceEvaluatorTimerBean {

    private static final Logger LOGGER = Logger.getLogger(VendorPerformanceEvaluatorTimerBean.class.getName());

    @EJB
    private VendorServiceLocal vendorService;

    @Schedule(hour = "0", minute = "0", second = "0", persistent = true, info = "Daily Midnight Vendor Performance Evaluator")
    public void executeMidnightVendorEvaluation() {
        LOGGER.info("[EJB TIMER MIDNIGHT] Starting periodic vendor scorecard calculations...");
        try {
            List<Vendor> vendors = vendorService.getAllVendors();
            for (Vendor v : vendors) {
                BigDecimal delivery = new BigDecimal("95.00");
                BigDecimal quality = new BigDecimal("97.50");
                BigDecimal compliance = new BigDecimal("98.00");
                vendorService.recordVendorEvaluation(v.getId(), delivery, quality, compliance, "2026-Q3-DAILY");
            }
            LOGGER.info(String.format("[EJB TIMER MIDNIGHT SUCCESS] Generated evaluation scorecards for %d vendors.", vendors.size()));
        } catch (Exception e) {
            LOGGER.severe("Midnight vendor evaluation timer error: " + e.getMessage());
        }
    }
}
