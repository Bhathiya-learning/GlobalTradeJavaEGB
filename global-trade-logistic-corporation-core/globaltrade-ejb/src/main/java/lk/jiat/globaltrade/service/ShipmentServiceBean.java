package lk.jiat.globaltrade.service;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.jiat.globaltrade.annotation.LogisticsAudited;
import lk.jiat.globaltrade.annotation.PerformanceMonitored;
import lk.jiat.globaltrade.entity.*;
import lk.jiat.globaltrade.exception.GlobalTradeException;
import lk.jiat.globaltrade.exception.InsufficientInventoryException;
import lk.jiat.globaltrade.interceptor.LogisticsAuditInterceptor;
import lk.jiat.globaltrade.interceptor.PerformanceMonitorInterceptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
@DeclareRoles({"ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_OFFICIAL", "VENDOR_REPRESENTATIVE", "CUSTOMER"})
@Interceptors({LogisticsAuditInterceptor.class, PerformanceMonitorInterceptor.class})
public class ShipmentServiceBean implements ShipmentServiceLocal, ShipmentServiceRemote {

    private static final Logger LOGGER = Logger.getLogger(ShipmentServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @RolesAllowed({"ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_OFFICIAL"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Shipment> getAllShipments() {
        return em.createQuery("SELECT s FROM Shipment s ORDER BY s.id DESC", Shipment.class).getResultList();
    }

    @Override
    @RolesAllowed({"ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_OFFICIAL", "CUSTOMER"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Shipment> getAllShipmentsRemote() {
        return getAllShipments();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Shipment getShipmentById(Long id) {
        return em.find(Shipment.class, id);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Shipment getShipmentByTrackingNumber(String trackingNumber) {
        List<Shipment> list = em.createQuery("SELECT s FROM Shipment s WHERE s.trackingNumber = :tn", Shipment.class)
                               .setParameter("tn", trackingNumber)
                               .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Shipment getShipmentByTrackingNumberRemote(String trackingNumber) {
        return getShipmentByTrackingNumber(trackingNumber);
    }

    @Override
    @LogisticsAudited(actionCode = "SHIPMENT_CREATE", targetEntity = "Shipment")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment createShipment(Long originWarehouseId, Long destinationAddressId, Long carrierId, List<Long> itemIds, List<Integer> quantities, Long userId) throws GlobalTradeException {
        Warehouse origin = em.find(Warehouse.class, originWarehouseId);
        Address dest = em.find(Address.class, destinationAddressId);
        Carrier carrier = (carrierId != null && carrierId > 0) ? em.find(Carrier.class, carrierId) : em.find(Carrier.class, 1L);
        User user = (userId != null) ? em.find(User.class, userId) : null;

        if (origin == null || dest == null) {
            throw new GlobalTradeException("Invalid warehouse or address ID for shipment creation.");
        }

        ShipmentStatus status = em.createQuery("SELECT s FROM ShipmentStatus s WHERE s.code = 'PLANNED'", ShipmentStatus.class).getSingleResult();
        String trackingNum = "GTL-2026-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Shipment shipment = new Shipment(null, trackingNum, origin, dest, carrier, status, null, LocalDateTime.now().plusDays(7), null, user);
        em.persist(shipment);

        for (int i = 0; i < itemIds.size(); i++) {
            InventoryItem item = em.find(InventoryItem.class, itemIds.get(i));
            int qty = quantities.get(i);
            if (item != null && qty > 0) {
                ShipmentItem sItem = new ShipmentItem(null, shipment, item, qty, item.getUnitPrice());
                em.persist(sItem);
                shipment.addShipmentItem(sItem);
            }
        }

        LOGGER.info("[EJB CMT SUCCESS] Created shipment tracking number: " + trackingNum + " by User ID: " + userId);
        return shipment;
    }

    @Override
    @LogisticsAudited(actionCode = "SHIPMENT_ASSIGN_CARRIER", targetEntity = "Shipment")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment assignCarrierAndDispatch(Long shipmentId, Long carrierId, String statusCode) throws GlobalTradeException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new GlobalTradeException("Shipment not found for ID: " + shipmentId);
        }
        if (carrierId != null && carrierId > 0) {
            Carrier carrier = em.find(Carrier.class, carrierId);
            if (carrier != null) {
                shipment.setCarrier(carrier);
            }
        }
        String statusToSet = (statusCode != null && !statusCode.isEmpty()) ? statusCode : "IN_TRANSIT";
        ShipmentStatus status = em.createQuery("SELECT s FROM ShipmentStatus s WHERE s.code = :code", ShipmentStatus.class)
                                  .setParameter("code", statusToSet)
                                  .getSingleResult();
        shipment.setStatus(status);
        shipment.setDispatchDate(LocalDateTime.now());
        em.merge(shipment);
        LOGGER.info("[CARRIER ASSIGNED & DISPATCHED] Shipment ID " + shipmentId + " assigned carrier " + carrierId + " status " + statusToSet);
        return shipment;
    }

    @Override
    @RolesAllowed({"ADMIN", "LOGISTICS_COORDINATOR"})
    @LogisticsAudited(actionCode = "SHIPMENT_DISPATCH", targetEntity = "Shipment")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment dispatchShipment(Long shipmentId) throws GlobalTradeException, InsufficientInventoryException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new GlobalTradeException("Shipment not found for ID: " + shipmentId);
        }

        // Check inventory for all line items
        for (ShipmentItem line : shipment.getShipmentItems()) {
            InventoryItem stock = line.getInventoryItem();
            if (stock.getStockLevel() < line.getQuantity()) {
                LOGGER.severe(String.format("[EJB CMT ROLLBACK] Insufficient stock for SKU %s (Available: %d, Required: %d)", stock.getSku(), stock.getStockLevel(), line.getQuantity()));
                throw new InsufficientInventoryException(stock.getSku(), line.getQuantity(), stock.getStockLevel());
            }
        }

        // Deduct inventory atomically
        for (ShipmentItem line : shipment.getShipmentItems()) {
            InventoryItem stock = line.getInventoryItem();
            stock.setStockLevel(stock.getStockLevel() - line.getQuantity());
            em.merge(stock);
        }

        ShipmentStatus status = em.createQuery("SELECT s FROM ShipmentStatus s WHERE s.code = 'IN_TRANSIT'", ShipmentStatus.class).getSingleResult();
        shipment.setStatus(status);
        shipment.setDispatchDate(LocalDateTime.now());
        em.merge(shipment);

        LOGGER.info("[EJB CMT DISPATCH COMPLETED] Shipment ID " + shipmentId + " is now IN_TRANSIT");
        return shipment;
    }

    @Override
    @RolesAllowed({"ADMIN", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICIAL"})
    @LogisticsAudited(actionCode = "SHIPMENT_STATUS_UPDATE", targetEntity = "Shipment")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateShipmentStatus(Long shipmentId, String statusCode) throws GlobalTradeException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new GlobalTradeException("Shipment not found for ID: " + shipmentId);
        }

        ShipmentStatus newStatus = em.createQuery("SELECT s FROM ShipmentStatus s WHERE s.code = :code", ShipmentStatus.class)
                                     .setParameter("code", statusCode)
                                     .getSingleResult();
        shipment.setStatus(newStatus);
        if ("DELIVERED".equals(statusCode)) {
            shipment.setActualDelivery(LocalDateTime.now());
        }
        em.merge(shipment);
        LOGGER.info("[SHIPMENT STATUS UPDATED] ID " + shipmentId + " -> " + statusCode);
    }
}
