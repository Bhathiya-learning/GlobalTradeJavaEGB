package lk.jiat.globaltrade.service;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.logging.Logger;
import lk.jiat.globaltrade.entity.SupportMessage;
import lk.jiat.globaltrade.entity.SupportTicket;
import lk.jiat.globaltrade.entity.User;
import lk.jiat.globaltrade.exception.GlobalTradeException;

@Stateless
@PermitAll
public class SupportServiceBean implements SupportServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(SupportServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SupportTicket createTicket(Long userId, String subject, String category, String shipmentTracking, String initialMessage) throws GlobalTradeException {
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new GlobalTradeException("User not found for support ticket creation.");
        }

        String ticketNumber = "TKT-" + System.currentTimeMillis() % 1000000;
        SupportTicket ticket = new SupportTicket(null, ticketNumber, user, subject, category != null ? category : "General Inquiry", shipmentTracking, "OPEN");

        em.persist(ticket);
        em.flush();

        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            String senderName = user.getFirstName() + " " + user.getLastName();
            String roleCode = user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
            SupportMessage msg = new SupportMessage(null, ticket, user, senderName, roleCode, initialMessage.trim());
            em.persist(msg);
        }

        LOGGER.info(String.format("[SUPPORT TICKET CREATED] %s by User %s (%s)", ticketNumber, user.getEmail(), subject));
        return ticket;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<SupportTicket> getTicketsForUser(Long userId) {
        return em.createQuery("SELECT DISTINCT t FROM SupportTicket t LEFT JOIN FETCH t.messages m WHERE t.user.id = :userId ORDER BY t.id DESC", SupportTicket.class)
                 .setParameter("userId", userId)
                 .getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<SupportTicket> getAllTickets() {
        return em.createQuery("SELECT DISTINCT t FROM SupportTicket t LEFT JOIN FETCH t.messages m ORDER BY t.id DESC", SupportTicket.class)
                 .getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public SupportTicket getTicketById(Long ticketId) {
        try {
            return em.createQuery("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.messages m WHERE t.id = :id", SupportTicket.class)
                     .setParameter("id", ticketId)
                     .getSingleResult();
        } catch (Exception e) {
            return em.find(SupportTicket.class, ticketId);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SupportMessage addMessage(Long ticketId, Long senderUserId, String senderName, String senderRole, String messageText) throws GlobalTradeException {
        SupportTicket ticket = em.find(SupportTicket.class, ticketId);
        if (ticket == null) {
            throw new GlobalTradeException("Support ticket not found.");
        }

        User sender = em.find(User.class, senderUserId != null ? senderUserId : ticket.getUser().getId());
        String name = senderName != null && !senderName.trim().isEmpty() ? senderName : (sender != null ? sender.getFirstName() + " " + sender.getLastName() : "System");
        String role = senderRole != null && !senderRole.trim().isEmpty() ? senderRole : "CUSTOMER";

        SupportMessage msg = new SupportMessage(null, ticket, sender, name, role, messageText.trim());
        em.persist(msg);
        if (ticket.getMessages() != null) {
            ticket.getMessages().add(msg);
        }
        em.flush();

        if ("ADMIN".equalsIgnoreCase(role) || "LOGISTICS_MGR".equalsIgnoreCase(role)) {
            ticket.setStatus("IN_PROGRESS");
        } else if ("CUSTOMER".equalsIgnoreCase(role) && "RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("OPEN");
        }

        em.merge(ticket);
        LOGGER.info(String.format("[SUPPORT MESSAGE ADDED] Ticket %s from %s (%s)", ticket.getTicketNumber(), name, role));
        return msg;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SupportTicket updateTicketStatus(Long ticketId, String status) throws GlobalTradeException {
        SupportTicket ticket = em.find(SupportTicket.class, ticketId);
        if (ticket == null) {
            throw new GlobalTradeException("Support ticket not found.");
        }

        ticket.setStatus(status.toUpperCase());
        em.merge(ticket);
        LOGGER.info(String.format("[SUPPORT TICKET STATUS CHANGED] Ticket %s -> %s", ticket.getTicketNumber(), status));
        return ticket;
    }
}
