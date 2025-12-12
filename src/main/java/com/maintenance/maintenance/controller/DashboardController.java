package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.service.FirebaseRealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                           Model model,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            redirectAttributes.addFlashAttribute("error", "Votre session a expiré. Veuillez vous reconnecter.");
            return "redirect:/login";
        }
        
        // Vérifier si la session est expirée
        try {
            Long sessionCreated = (Long) session.getAttribute("sessionCreated");
            if (sessionCreated != null) {
                long sessionAge = System.currentTimeMillis() - sessionCreated;
                long sessionMaxAge = 1800 * 1000; // 30 minutes
                if (sessionAge > sessionMaxAge) {
                    // Session expirée, l'invalider et rediriger
                    try {
                        session.invalidate();
                    } catch (Exception e) {
                        // Ignorer les erreurs d'invalidation
                    }
                    redirectAttributes.addFlashAttribute("error", "Votre session a expiré. Veuillez vous reconnecter.");
                    return "redirect:/login";
                }
            }
        } catch (IllegalStateException e) {
            // Session déjà invalidée
            redirectAttributes.addFlashAttribute("error", "Votre session a expiré. Veuillez vous reconnecter.");
            return "redirect:/login";
        }
        
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour accéder à cette page.");
            return "redirect:/login";
        }

        try {
            session.setAttribute("authenticated", true);
            
            // Récupérer toutes les entreprises
            List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
            if (enterprises == null) {
                enterprises = new java.util.ArrayList<>();
            }

            // Si aucune entreprise sélectionnée, utiliser la dernière ou la première
            if (!StringUtils.hasText(entrepriseId)) {
                String lastEntrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
                if (StringUtils.hasText(lastEntrepriseId)) {
                    entrepriseId = lastEntrepriseId;
                } else if (!enterprises.isEmpty()) {
                    entrepriseId = enterprises.get(0).get("entrepriseId").toString();
                }
            }

            // Calculer les statistiques
            Map<String, Object> stats = calculateStatistics(entrepriseId);
            
            model.addAttribute("enterprises", enterprises);
            model.addAttribute("selectedEntrepriseId", entrepriseId != null ? entrepriseId : "");
            model.addAttribute("stats", stats);
            
            if (StringUtils.hasText(entrepriseId)) {
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            }

            return "index";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement du dashboard: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/dashboard/api/stats")
    @ResponseBody
    public Map<String, Object> getStats(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                                        HttpServletRequest request) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats = calculateStatistics(entrepriseId);
        } catch (Exception e) {
            e.printStackTrace();
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private Map<String, Object> calculateStatistics(String entrepriseId) throws Exception {
        Map<String, Object> stats = new HashMap<>();
        
        int totalMachines = 0;
        int openTickets = 0;
        int pendingMaintenance = 0;
        int activeRepairs = 0;
        int completedMaintenance = 0;
        int urgentTickets = 0;
        int pendingRappels = 0;
        int pendingAlertes = 0;
        
        if (StringUtils.hasText(entrepriseId)) {
            // Compter les machines
            try {
                List<com.maintenance.maintenance.model.entity.Machine> machines = 
                    firebaseRealtimeService.getMachinesForEnterprise(entrepriseId);
                totalMachines = machines != null ? machines.size() : 0;
            } catch (Exception e) {
                System.err.println("Erreur lors du comptage des machines: " + e.getMessage());
            }
            
            // Compter les tickets
            try {
                List<com.maintenance.maintenance.model.entity.Ticket> tickets = 
                    firebaseRealtimeService.getTicketsForEnterprise(entrepriseId);
                if (tickets != null) {
                    for (com.maintenance.maintenance.model.entity.Ticket ticket : tickets) {
                        String statut = ticket.getStatut();
                        if (statut != null && (statut.equals("a_faire") || statut.equals("en_cours"))) {
                            openTickets++;
                        }
                        if (ticket.getPriorite() != null && ticket.getPriorite().equals("urgente") 
                            && (statut != null && (statut.equals("a_faire") || statut.equals("en_cours")))) {
                            urgentTickets++;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du comptage des tickets: " + e.getMessage());
            }
            
            // Compter les rappels non vérifiés
            try {
                List<com.maintenance.maintenance.model.entity.Rappel> rappels = 
                    firebaseRealtimeService.getRappelsForEnterprise(entrepriseId);
                if (rappels != null) {
                    for (com.maintenance.maintenance.model.entity.Rappel rappel : rappels) {
                        if (rappel.getVerifie() == null || !rappel.getVerifie()) {
                            pendingRappels++;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du comptage des rappels: " + e.getMessage());
            }
            
            // Compter les alertes non vérifiées
            try {
                List<com.maintenance.maintenance.model.entity.Alerte> alertes = 
                    firebaseRealtimeService.getAlertesForEnterprise(entrepriseId);
                if (alertes != null) {
                    for (com.maintenance.maintenance.model.entity.Alerte alerte : alertes) {
                        if (alerte.getVerifie() == null || !alerte.getVerifie()) {
                            pendingAlertes++;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du comptage des alertes: " + e.getMessage());
            }
            
            // Compter les maintenances complétées (historique)
            try {
                List<com.maintenance.maintenance.model.entity.HistoriqueVerification> historiques = 
                    firebaseRealtimeService.getHistoriqueVerificationsForEnterprise(entrepriseId);
                completedMaintenance = historiques != null ? historiques.size() : 0;
            } catch (Exception e) {
                System.err.println("Erreur lors du comptage de l'historique: " + e.getMessage());
            }
            
            // Maintenances en attente = rappels + alertes non vérifiés
            pendingMaintenance = pendingRappels + pendingAlertes;
        }
        
        stats.put("totalMachines", totalMachines);
        stats.put("openTickets", openTickets);
        stats.put("pendingMaintenance", pendingMaintenance);
        stats.put("activeRepairs", activeRepairs);
        stats.put("completedMaintenance", completedMaintenance);
        stats.put("urgentTickets", urgentTickets);
        stats.put("pendingRappels", pendingRappels);
        stats.put("pendingAlertes", pendingAlertes);
        
        return stats;
    }
}

