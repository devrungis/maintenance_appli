package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.service.FirebaseRealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/rapports")
public class RapportsController {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @GetMapping
    public String rapports(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                          Model model,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour accéder à cette page.");
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
                enterprises = new ArrayList<>();
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

            // Calculer les données pour les graphiques
            Map<String, Object> reportData = calculateReportData(entrepriseId);
            
            model.addAttribute("enterprises", enterprises);
            model.addAttribute("selectedEntrepriseId", entrepriseId != null ? entrepriseId : "");
            model.addAttribute("reportData", reportData);
            
            if (StringUtils.hasText(entrepriseId)) {
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            }

            return "rapports/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des rapports: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/api/data")
    @ResponseBody
    public Map<String, Object> getReportData(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                                             HttpServletRequest request) {
        Map<String, Object> reportData = new HashMap<>();
        try {
            reportData = calculateReportData(entrepriseId);
        } catch (Exception e) {
            e.printStackTrace();
            reportData.put("error", e.getMessage());
        }
        return reportData;
    }

    private Map<String, Object> calculateReportData(String entrepriseId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        
        if (!StringUtils.hasText(entrepriseId)) {
            return data;
        }
        
        // Statistiques des tickets par statut
        Map<String, Integer> ticketsByStatus = new HashMap<>();
        Map<String, Integer> ticketsByPriority = new HashMap<>();
        
        // Statistiques des machines
        int totalMachines = 0;
        int machinesOperationnelles = 0;
        int machinesEnReparation = 0;
        
        // Statistiques des rappels
        Map<String, Integer> rappelsByStatus = new HashMap<>();
        rappelsByStatus.put("envoye", 0);
        rappelsByStatus.put("non_envoye", 0);
        rappelsByStatus.put("verifie", 0);
        
        // Statistiques des alertes
        Map<String, Integer> alertesByStatus = new HashMap<>();
        alertesByStatus.put("envoye", 0);
        alertesByStatus.put("non_envoye", 0);
        alertesByStatus.put("verifie", 0);
        
        // Statistiques mensuelles (derniers 6 mois)
        Map<String, Map<String, Integer>> monthlyStats = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 5; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, -i);
            String monthKey = String.format("%02d/%d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
            monthlyStats.put(monthKey, new HashMap<>());
            monthlyStats.get(monthKey).put("tickets", 0);
            monthlyStats.get(monthKey).put("rappels", 0);
            monthlyStats.get(monthKey).put("alertes", 0);
        }
        
        try {
            // Compter les machines
            List<com.maintenance.maintenance.model.entity.Machine> machines = 
                firebaseRealtimeService.getMachinesForEnterprise(entrepriseId);
            if (machines != null) {
                totalMachines = machines.size();
                for (com.maintenance.maintenance.model.entity.Machine machine : machines) {
                    if (machine.getOperationnel() != null && machine.getOperationnel()) {
                        machinesOperationnelles++;
                    }
                    if (machine.getEnReparation() != null && machine.getEnReparation()) {
                        machinesEnReparation++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul des statistiques machines: " + e.getMessage());
        }
        
        try {
            // Compter les tickets
            List<com.maintenance.maintenance.model.entity.Ticket> tickets = 
                firebaseRealtimeService.getTicketsForEnterprise(entrepriseId);
            if (tickets != null) {
                for (com.maintenance.maintenance.model.entity.Ticket ticket : tickets) {
                    // Par statut
                    String statut = ticket.getStatut() != null ? ticket.getStatut() : "unknown";
                    ticketsByStatus.put(statut, ticketsByStatus.getOrDefault(statut, 0) + 1);
                    
                    // Par priorité
                    String priorite = ticket.getPriorite() != null ? ticket.getPriorite() : "unknown";
                    ticketsByPriority.put(priorite, ticketsByPriority.getOrDefault(priorite, 0) + 1);
                    
                    // Par mois
                    if (ticket.getDateCreation() != null) {
                        Calendar ticketCal = Calendar.getInstance();
                        ticketCal.setTimeInMillis(ticket.getDateCreation());
                        String monthKey = String.format("%02d/%d", ticketCal.get(Calendar.MONTH) + 1, ticketCal.get(Calendar.YEAR));
                        if (monthlyStats.containsKey(monthKey)) {
                            monthlyStats.get(monthKey).put("tickets", monthlyStats.get(monthKey).get("tickets") + 1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul des statistiques tickets: " + e.getMessage());
        }
        
        try {
            // Compter les rappels
            List<com.maintenance.maintenance.model.entity.Rappel> rappels = 
                firebaseRealtimeService.getRappelsForEnterprise(entrepriseId);
            if (rappels != null) {
                for (com.maintenance.maintenance.model.entity.Rappel rappel : rappels) {
                    if (rappel.getVerifie() != null && rappel.getVerifie()) {
                        rappelsByStatus.put("verifie", rappelsByStatus.get("verifie") + 1);
                    } else if (rappel.getEnvoye() != null && rappel.getEnvoye()) {
                        rappelsByStatus.put("envoye", rappelsByStatus.get("envoye") + 1);
                    } else {
                        rappelsByStatus.put("non_envoye", rappelsByStatus.get("non_envoye") + 1);
                    }
                    
                    // Par mois
                    if (rappel.getDateCreation() != null) {
                        Calendar rappelCal = Calendar.getInstance();
                        rappelCal.setTimeInMillis(rappel.getDateCreation());
                        String monthKey = String.format("%02d/%d", rappelCal.get(Calendar.MONTH) + 1, rappelCal.get(Calendar.YEAR));
                        if (monthlyStats.containsKey(monthKey)) {
                            monthlyStats.get(monthKey).put("rappels", monthlyStats.get(monthKey).get("rappels") + 1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul des statistiques rappels: " + e.getMessage());
        }
        
        try {
            // Compter les alertes
            List<com.maintenance.maintenance.model.entity.Alerte> alertes = 
                firebaseRealtimeService.getAlertesForEnterprise(entrepriseId);
            if (alertes != null) {
                for (com.maintenance.maintenance.model.entity.Alerte alerte : alertes) {
                    if (alerte.getVerifie() != null && alerte.getVerifie()) {
                        alertesByStatus.put("verifie", alertesByStatus.get("verifie") + 1);
                    } else if (alerte.getEnvoye() != null && alerte.getEnvoye()) {
                        alertesByStatus.put("envoye", alertesByStatus.get("envoye") + 1);
                    } else {
                        alertesByStatus.put("non_envoye", alertesByStatus.get("non_envoye") + 1);
                    }
                    
                    // Par mois
                    if (alerte.getDateCreation() != null) {
                        Calendar alerteCal = Calendar.getInstance();
                        alerteCal.setTimeInMillis(alerte.getDateCreation());
                        String monthKey = String.format("%02d/%d", alerteCal.get(Calendar.MONTH) + 1, alerteCal.get(Calendar.YEAR));
                        if (monthlyStats.containsKey(monthKey)) {
                            monthlyStats.get(monthKey).put("alertes", monthlyStats.get(monthKey).get("alertes") + 1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul des statistiques alertes: " + e.getMessage());
        }
        
        data.put("totalMachines", totalMachines);
        data.put("machinesOperationnelles", machinesOperationnelles);
        data.put("machinesEnReparation", machinesEnReparation);
        data.put("ticketsByStatus", ticketsByStatus);
        data.put("ticketsByPriority", ticketsByPriority);
        data.put("rappelsByStatus", rappelsByStatus);
        data.put("alertesByStatus", alertesByStatus);
        data.put("monthlyStats", monthlyStats);
        
        return data;
    }
}

