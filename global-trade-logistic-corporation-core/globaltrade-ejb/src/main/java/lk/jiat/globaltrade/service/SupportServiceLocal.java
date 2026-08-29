package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import java.util.List;
import lk.jiat.globaltrade.entity.SupportMessage;
import lk.jiat.globaltrade.entity.SupportTicket;
import lk.jiat.globaltrade.exception.GlobalTradeException;

@Local
public interface SupportServiceLocal {

    SupportTicket createTicket(Long userId, String subject, String category, String shipmentTracking, String initialMessage) throws GlobalTradeException;

    List<SupportTicket> getTicketsForUser(Long userId);

    List<SupportTicket> getAllTickets();

    SupportTicket getTicketById(Long ticketId);

    SupportMessage addMessage(Long ticketId, Long senderUserId, String senderName, String senderRole, String messageText) throws GlobalTradeException;

    SupportTicket updateTicketStatus(Long ticketId, String status) throws GlobalTradeException;
}
