package lk.jiat.globaltrade.timer;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import lk.jiat.globaltrade.entity.CustomsDeclaration;
import lk.jiat.globaltrade.service.CustomsClearanceServiceLocal;

import java.util.List;
import java.util.logging.Logger;

@Stateless
public class CustomsComplianceTimerBean {

    private static final Logger LOGGER = Logger.getLogger(CustomsComplianceTimerBean.class.getName());

    @EJB
    private CustomsClearanceServiceLocal customsService;

    @Schedule(minute = "*/30", hour = "*", persistent = false, info = "30-Minute Customs Declaration Compliance Monitor")
    public void monitorCustomsDeclarations() {
        LOGGER.info("[EJB TIMER AUTOMATIC] Scanning active customs declarations for regulatory compliance...");
        try {
            List<CustomsDeclaration> list = customsService.getAllDeclarations();
            int pendingCount = 0;
            for (CustomsDeclaration decl : list) {
                if ("PENDING".equals(decl.getCustomsStatus().getCode()) || "INSPECTION_REQUIRED".equals(decl.getCustomsStatus().getCode())) {
                    pendingCount++;
                }
            }
            LOGGER.info(String.format("[EJB TIMER SUCCESS] Monitored %d customs declarations (%d requiring action).", list.size(), pendingCount));
        } catch (Exception e) {
            LOGGER.severe("Customs compliance monitor timer error: " + e.getMessage());
        }
    }
}
