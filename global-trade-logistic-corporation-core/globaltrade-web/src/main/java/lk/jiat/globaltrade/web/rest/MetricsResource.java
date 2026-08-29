package lk.jiat.globaltrade.web.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.jiat.globaltrade.service.PerformanceMetricsServiceLocal;

import javax.naming.InitialContext;
import java.util.Map;

@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetricsResource {

    @EJB
    private PerformanceMetricsServiceLocal metricsService;

    private PerformanceMetricsServiceLocal getService() {
        if (metricsService != null) {
            return metricsService;
        }
        try {
            InitialContext ctx = new InitialContext();
            return (PerformanceMetricsServiceLocal) ctx.lookup("java:global/globaltrade-ear-1.0/lk.jiat.globaltrade-globaltrade-ejb-1.0-SNAPSHOT/PerformanceMetricsServiceBean!lk.jiat.globaltrade.service.PerformanceMetricsServiceLocal");
        } catch (Exception e) {
            return null;
        }
    }

    @GET
    public Response getMetrics() {
        try {
            PerformanceMetricsServiceLocal service = getService();
            if (service != null) {
                Map<String, Object> metrics = service.getSystemMetrics();
                return Response.ok(metrics).build();
            }
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("error", "EJB Metrics service unavailable")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
