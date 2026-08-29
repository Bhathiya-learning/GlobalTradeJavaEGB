package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import lk.jiat.globaltrade.entity.Shipment;
import lk.jiat.globaltrade.exception.GlobalTradeException;
import lk.jiat.globaltrade.exception.InsufficientInventoryException;

import java.util.List;

@Local
public interface ShipmentServiceLocal {
    List<Shipment> getAllShipments();
    Shipment getShipmentById(Long id);
    Shipment getShipmentByTrackingNumber(String trackingNumber);
    Shipment createShipment(Long originWarehouseId, Long destinationAddressId, Long carrierId, List<Long> itemIds, List<Integer> quantities, Long userId) throws GlobalTradeException;
    Shipment dispatchShipment(Long shipmentId) throws GlobalTradeException, InsufficientInventoryException;
    Shipment assignCarrierAndDispatch(Long shipmentId, Long carrierId, String statusCode) throws GlobalTradeException;
    void updateShipmentStatus(Long shipmentId, String statusName) throws GlobalTradeException;
}
