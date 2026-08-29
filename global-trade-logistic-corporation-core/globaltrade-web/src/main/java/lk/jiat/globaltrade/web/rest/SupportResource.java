package lk.jiat.globaltrade.web.rest;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lk.jiat.globaltrade.entity.SupportMessage;
import lk.jiat.globaltrade.entity.SupportTicket;
import lk.jiat.globaltrade.service.SupportServiceLocal;

@Path("/support")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class SupportResource {

    @EJB
    private SupportServiceLocal supportService;

    @GET
    @Path("/tickets")
    public Response getTickets(@QueryParam("userId") Long userId) {
        try {
            System.out.println("[SERVER TELEMETRY LOG] GET /api/support/tickets requested for userId=" + userId);
            List<SupportTicket> list = (userId != null && userId > 0)
                    ? supportService.getTicketsForUser(userId)
                    : supportService.getAllTickets();

            System.out.println("[SERVER TELEMETRY LOG] Retreived " + (list != null ? list.size() : 0) + " support tickets from database.");
            List<Map<String, Object>> response = new ArrayList<>();
            for (SupportTicket t : list) {
                response.add(serializeTicket(t));
            }

            return Response.ok(response).build();
        } catch (Exception e) {
            System.err.println("[SERVER ERROR LOG] GET /api/support/tickets error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/tickets/{id}")
    public Response getTicketById(@PathParam("id") Long id) {
        try {
            System.out.println("[SERVER TELEMETRY LOG] GET /api/support/tickets/" + id + " requested.");
            SupportTicket ticket = supportService.getTicketById(id);
            if (ticket == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Support ticket not found"))
                        .build();
            }
            return Response.ok(serializeTicket(ticket)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/tickets/create")
    public Response createTicket(Map<String, Object> payload) {
        try {
            System.out.println("[SERVER TELEMETRY LOG] POST /api/support/tickets/create payload: " + payload);
            if (payload == null || !payload.containsKey("userId") || !payload.containsKey("subject")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "userId and subject are required"))
                        .build();
            }

            Long userId = Long.valueOf(payload.get("userId").toString());
            String subject = payload.get("subject").toString().trim();
            String category = payload.containsKey("category") ? payload.get("category").toString() : "General Inquiry";
            String tracking = payload.containsKey("shipmentTracking") ? payload.get("shipmentTracking").toString() : null;
            String initialMessage = payload.containsKey("message") ? payload.get("message").toString() : subject;

            SupportTicket ticket = supportService.createTicket(userId, subject, category, tracking, initialMessage);
            return Response.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Support inquiry ticket created",
                    "ticket", serializeTicket(ticket)
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/tickets/{id}/reply")
    public Response addReply(@PathParam("id") Long id, Map<String, Object> payload) {
        try {
            if (payload == null || !payload.containsKey("message")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "message is required"))
                        .build();
            }

            Long senderUserId = payload.containsKey("senderUserId") ? Long.valueOf(payload.get("senderUserId").toString()) : null;
            String senderName = payload.containsKey("senderName") ? payload.get("senderName").toString() : null;
            String senderRole = payload.containsKey("senderRole") ? payload.get("senderRole").toString() : "CUSTOMER";
            String messageText = payload.get("message").toString().trim();

            SupportMessage msg = supportService.addMessage(id, senderUserId, senderName, senderRole, messageText);
            SupportTicket ticket = supportService.getTicketById(id);

            return Response.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Reply message added",
                    "ticket", serializeTicket(ticket)
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/tickets/{id}/status")
    public Response updateStatus(@PathParam("id") Long id, Map<String, Object> payload) {
        try {
            if (payload == null || !payload.containsKey("status")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "status is required"))
                        .build();
            }

            String status = payload.get("status").toString().trim();
            SupportTicket ticket = supportService.updateTicketStatus(id, status);

            return Response.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Ticket status updated to " + ticket.getStatus(),
                    "ticket", serializeTicket(ticket)
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    private Map<String, Object> serializeTicket(SupportTicket t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("ticketNumber", t.getTicketNumber() != null ? t.getTicketNumber() : ("TKT-" + t.getId()));
        map.put("userId", t.getUser() != null ? t.getUser().getId() : null);
        map.put("userName", t.getUser() != null ? (t.getUser().getFirstName() + " " + t.getUser().getLastName()) : "Customer");
        map.put("userEmail", t.getUser() != null ? t.getUser().getEmail() : "");
        map.put("subject", t.getSubject());
        map.put("category", t.getCategory());
        map.put("shipmentTracking", t.getShipmentTracking());
        map.put("status", t.getStatus());
        map.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        map.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : "");

        List<Map<String, Object>> msgs = new ArrayList<>();
        if (t.getMessages() != null) {
            for (SupportMessage m : t.getMessages()) {
                Map<String, Object> mm = new HashMap<>();
                mm.put("id", m.getId());
                mm.put("senderName", m.getSenderName());
                mm.put("senderRole", m.getSenderRole());
                mm.put("senderUserId", m.getSenderUser() != null ? m.getSenderUser().getId() : null);
                mm.put("message", m.getMessage());
                mm.put("sentAt", m.getSentAt() != null ? m.getSentAt().toString() : "");
                msgs.add(mm);
            }
        }
        map.put("messages", msgs);
        return map;
    }
}
