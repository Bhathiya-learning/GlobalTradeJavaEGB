package lk.jiat.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/tariffs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TariffConfigResource {

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;



    @GET
    @Path("/rates")
    public Response getTariffRates() {
        Map<String, Object> rates = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {

            // 1. Fetch Global Rates & Defaults
            String sqlGlobal = "SELECT rate_key, rate_value FROM tariff_rates";
            try (PreparedStatement ps = conn.prepareStatement(sqlGlobal);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rates.put(rs.getString("rate_key"), rs.getDouble("rate_value"));
                }
            }

            // 2. Fetch Structured Relational Route Tariffs from route_tariffs table
            String sqlRoutes = "SELECT origin_warehouse_id, destination_warehouse_id, base_tariff FROM route_tariffs";
            try (PreparedStatement psRoutes = conn.prepareStatement(sqlRoutes);
                 ResultSet rsRoutes = psRoutes.executeQuery()) {
                while (rsRoutes.next()) {
                    long oId = rsRoutes.getLong("origin_warehouse_id");
                    long dId = rsRoutes.getLong("destination_warehouse_id");
                    double tariff = rsRoutes.getDouble("base_tariff");
                    rates.put("route_" + oId + "_" + dId, tariff);
                    rates.put("route_" + dId + "_" + oId, tariff);
                }
            }

            return Response.ok(rates).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to load tariff rates from MySQL database: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/rates")
    public Response updateTariffRates(Map<String, Object> newRates) {
        if (newRates == null || newRates.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Rates payload cannot be empty")).build();
        }

        try (Connection conn = dataSource.getConnection()) {

            String sqlTariffRates = "INSERT INTO tariff_rates (rate_key, rate_value, description) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE rate_value = VALUES(rate_value)";
            String sqlRouteTariffs = "INSERT INTO route_tariffs (origin_warehouse_id, destination_warehouse_id, base_tariff) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE base_tariff = VALUES(base_tariff)";

            try (PreparedStatement psRates = conn.prepareStatement(sqlTariffRates);
                 PreparedStatement psRoutes = conn.prepareStatement(sqlRouteTariffs)) {

                for (Map.Entry<String, Object> entry : newRates.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        String key = entry.getKey().trim();
                        if (key.isEmpty() || key.equals("route__")) continue;

                        try {
                            double val = Double.parseDouble(entry.getValue().toString());
                            String desc = key.startsWith("route_") ? "Custom Route Tariff Rate (" + key + ")" : "Tariff Config (" + key + ")";
                            
                            // 1. Always update tariff_rates table
                            psRates.setString(1, key);
                            psRates.setDouble(2, val);
                            psRates.setString(3, desc);
                            psRates.addBatch();

                            // 2. If key matches route_{originId}_{destId}, update structured route_tariffs table
                            if (key.startsWith("route_") && !key.equals("route_default")) {
                                String[] parts = key.split("_");
                                if (parts.length == 3) {
                                    long oId = Long.parseLong(parts[1]);
                                    long dId = Long.parseLong(parts[2]);
                                    if (oId != dId) {
                                        psRoutes.setLong(1, oId);
                                        psRoutes.setLong(2, dId);
                                        psRoutes.setDouble(3, val);
                                        psRoutes.addBatch();
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }

                psRates.executeBatch();
                psRoutes.executeBatch();
            }

            return Response.ok(Map.of(
                "status", "SUCCESS",
                "message", "Route tariffs updated in MySQL relational database table `route_tariffs` successfully!"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to update tariff rates in MySQL database: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/locations")
    public Response getLocations() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT w.id, w.warehouse_code, w.name, a.address_line1, c.name as city_name, co.name as country_name " +
                     "FROM warehouses w " +
                     "LEFT JOIN addresses a ON w.address_id = a.id " +
                     "LEFT JOIN cities c ON a.city_id = c.id " +
                     "LEFT JOIN countries co ON c.country_id = co.id " +
                     "WHERE w.is_active = TRUE ORDER BY w.id ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> loc = new HashMap<>();
                long id = rs.getLong("id");
                String code = rs.getString("warehouse_code");
                String name = rs.getString("name");
                String city = rs.getString("city_name") != null ? rs.getString("city_name") : "Global";
                String country = rs.getString("country_name") != null ? rs.getString("country_name") : "Location";

                loc.put("id", id);
                loc.put("code", code);
                loc.put("name", name);
                loc.put("cityName", city);
                loc.put("countryName", country);
                loc.put("displayName", name + " (" + city + ", " + country + ")");
                list.add(loc);
            }
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to load dynamic locations from MySQL: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/locations")
    public Response createLocation(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("name")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Location name is required")).build();
        }

        String code = payload.getOrDefault("code", "WH-HUB-" + (System.currentTimeMillis() % 10000)).toString();
        String name = payload.get("name").toString();
        String cityName = payload.getOrDefault("cityName", "Global Hub").toString();
        String countryCode = payload.getOrDefault("countryCode", "US").toString();
        String addressLine = payload.getOrDefault("addressLine", "Main Logistics Complex").toString();
        double capacity = payload.containsKey("capacity") ? Double.parseDouble(payload.get("capacity").toString()) : 50000.0;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {

                // 1. Get or default Country ID
                long countryId = 1;
                String countrySql = "SELECT id FROM countries WHERE iso_code_2 = ? OR name = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(countrySql)) {
                    ps.setString(1, countryCode);
                    ps.setString(2, countryCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) countryId = rs.getLong("id");
                    }
                }

                // 2. Get or Insert City
                long cityId = 1;
                String citySelectSql = "SELECT id FROM cities WHERE name = ? LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(citySelectSql)) {
                    ps.setString(1, cityName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            cityId = rs.getLong("id");
                        } else {
                            String cityInsertSql = "INSERT INTO cities (country_id, name, postal_code) VALUES (?, ?, '00000')";
                            try (PreparedStatement psIns = conn.prepareStatement(cityInsertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                                psIns.setLong(1, countryId);
                                psIns.setString(2, cityName);
                                psIns.executeUpdate();
                                try (ResultSet rsKey = psIns.getGeneratedKeys()) {
                                    if (rsKey.next()) cityId = rsKey.getLong(1);
                                }
                            }
                        }
                    }
                }

                // 3. Insert Address
                long addressId = 1;
                String addrInsertSql = "INSERT INTO addresses (city_id, address_line1, address_line2) VALUES (?, ?, 'Freight Zone')";
                try (PreparedStatement psAddr = conn.prepareStatement(addrInsertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    psAddr.setLong(1, cityId);
                    psAddr.setString(2, addressLine);
                    psAddr.executeUpdate();
                    try (ResultSet rsKey = psAddr.getGeneratedKeys()) {
                        if (rsKey.next()) addressId = rsKey.getLong(1);
                    }
                }

                // 4. Insert Warehouse
                long warehouseId = 1;
                String whInsertSql = "INSERT INTO warehouses (warehouse_code, name, address_id, total_capacity_sqm, is_active) VALUES (?, ?, ?, ?, TRUE)";
                try (PreparedStatement psWh = conn.prepareStatement(whInsertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    psWh.setString(1, code);
                    psWh.setString(2, name);
                    psWh.setLong(3, addressId);
                    psWh.setDouble(4, capacity);
                    psWh.executeUpdate();
                    try (ResultSet rsKey = psWh.getGeneratedKeys()) {
                        if (rsKey.next()) warehouseId = rsKey.getLong(1);
                    }
                }

                conn.commit();
                return Response.ok(Map.of(
                    "status", "SUCCESS",
                    "id", warehouseId,
                    "code", code,
                    "name", name,
                    "message", "Registered new freight location '" + name + "' in MySQL database successfully!"
                )).build();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to register new location in MySQL: " + e.getMessage()))
                    .build();
        }
    }
}
