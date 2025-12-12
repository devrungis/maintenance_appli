package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    public List<Ticket> listTickets(String entrepriseId) throws Exception {
        return firebaseRealtimeService.getTicketsForEnterprise(entrepriseId);
    }

    public Ticket getTicket(String entrepriseId, String ticketId) throws Exception {
        return firebaseRealtimeService.getTicketById(entrepriseId, ticketId);
    }

    public String createTicket(String entrepriseId, Ticket ticket) throws Exception {
        return firebaseRealtimeService.createTicket(entrepriseId, ticket);
    }

    public void updateTicket(String entrepriseId, String ticketId, Ticket ticket) throws Exception {
        firebaseRealtimeService.updateTicket(entrepriseId, ticketId, ticket);
    }

    public void deleteTicket(String entrepriseId, String ticketId) throws Exception {
        firebaseRealtimeService.deleteTicket(entrepriseId, ticketId);
    }
}

