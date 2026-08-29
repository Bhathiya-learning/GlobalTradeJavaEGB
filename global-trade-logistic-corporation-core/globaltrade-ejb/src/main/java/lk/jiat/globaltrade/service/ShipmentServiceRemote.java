package lk.jiat.globaltrade.service;

import jakarta.ejb.Remote;
import lk.jiat.globaltrade.entity.Shipment;
import lk.jiat.globaltrade.exception.GlobalTradeException;

import java.util.List;

@Remote
public interface ShipmentServiceRemote {
    List<Shipment> getAllShipmentsRemote();
    Shipment getShipmentByTrackingNumberRemote(String trackingNumber);
}
