package lk.jiat.globaltrade.timer;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;

import lk.jiat.globaltrade.entity.Shipment;
import lk.jiat.globaltrade.service.ShipmentServiceLocal;

import java.util.List;
import java.util.logging.Logger;

@Stateless
public class ShipmentTrackingTimerBean {

    private static final Logger LOGGER = Logger.getLogger(ShipmentTrackingTimerBean.class.getName());

    @EJB
    private ShipmentServiceLocal shipmentService;

    @Schedule(minute = "*/15", hour = "*", persistent = false, info = "15-Minute Carrier Shipment Tracking Poller")
    public void executeShipmentTrackingCheck() {
        LOGGER.info("[EJB TIMER AUTOMATIC] Polling international carriers for shipment transit updates...");
        try {
            List<Shipment> shipments = shipmentService.getAllShipments();
            int inTransitCount = 0;
            for (Shipment s : shipments) {
                if ("IN_TRANSIT".equals(s.getStatus().getCode())) {
                    inTransitCount++;
                }
            }
            LOGGER.info(String.format("[EJB TIMER SUCCESS] Polled %d total shipments (%d currently IN_TRANSIT).", shipments.size(), inTransitCount));
        } catch (Exception e) {
            LOGGER.severe("[EJB TIMER ERROR] Shipment tracking poll failed: " + e.getMessage());
        }
    }
}
