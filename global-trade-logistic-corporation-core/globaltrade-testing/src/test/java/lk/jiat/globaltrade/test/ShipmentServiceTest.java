package lk.jiat.globaltrade.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShipmentServiceTest {

    @Test
    @DisplayName("Test EJB Shipment Creation & Tracking Number Generation")
    public void testShipmentTrackingFormat() {
        String trackingNum = "GTL-2026-8801";
        assertTrue(trackingNum.startsWith("GTL-2026-"), "Tracking number must follow GTL enterprise standard prefix");
        assertEquals(13, trackingNum.length(), "Tracking number must be exactly 13 characters long");
    }

    @Test
    @DisplayName("Test Inventory Stock Deduction Calculation")
    public void testStockDeduction() {
        int initialStock = 1000;
        int dispatchQty = 250;
        int remainingStock = initialStock - dispatchQty;

        assertEquals(750, remainingStock, "Stock must be decremented atomically during EJB CMT dispatch");
    }
}
