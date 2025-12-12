package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.entity.Alerte;
import com.maintenance.maintenance.model.entity.HistoriqueVerification;
import com.maintenance.maintenance.service.AlertService;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import com.maintenance.maintenance.service.MachineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/alertes")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private MachineService machineService;

    /**
     * Vérifie si l'utilisateur est connecté (tous les rôles autorisés)
     */
    private boolean ensureAuthenticated(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // Obtenir la session existante (ne pas en créer une nouvelle si elle n'existe pas)
        HttpSession session = request.getSession(false);
        
        // Si aucune session, l'utilisateur n'est pas authentifié
        if (session == null) {
            redirectAttributes.addFlashAttribute("error", "Votre session a expiré. Veuillez vous reconnecter.");
            return false;
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
                    return false;
                }
            }
        } catch (IllegalStateException e) {
            // Session déjà invalidée
            redirectAttributes.addFlashAttribute("error", "Votre session a expiré. Veuillez vous reconnecter.");
            return false;
        }
        
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null 
            && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
            && !"anonymousUser".equals(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())) {
            session.setAttribute("authenticated", true);
            // Mettre à jour le timestamp pour prolonger la session
            session.setAttribute("sessionCreated", System.currentTimeMillis());
            return true;
        }
        
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated != null && authenticated) {
            // Mettre à jour le timestamp pour prolonger la session
            session.setAttribute("sessionCreated", System.currentTimeMillis());
            return true;
        }
        
        redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour accéder à cette page.");
        return false;
    }

    /**
     * Vérifie si l'utilisateur est superadmin
     */
    private boolean isSuperAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String role = (String) session.getAttribute("role");
            return "superadmin".equalsIgnoreCase(role);
        }
        return false;
    }

    /**
     * Liste toutes les alertes
     */
    @GetMapping
    public String listAlertes(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
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

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut accéder à cette page.");
            return "redirect:/dashboard";
        }

        try {
            session.setAttribute("authenticated", true);
            
            List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
            if (enterprises == null) {
                enterprises = new ArrayList<>();
            }

            if (!StringUtils.hasText(entrepriseId)) {
                String lastEntrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
                if (StringUtils.hasText(lastEntrepriseId)) {
                    entrepriseId = lastEntrepriseId;
                } else if (!enterprises.isEmpty()) {
                    entrepriseId = enterprises.get(0).get("entrepriseId").toString();
                }
            }

            List<Alerte> alertes = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                try {
                    alertes = alertService.listAlertes(entrepriseId);
                    if (alertes == null) {
                        alertes = new ArrayList<>();
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération des alertes: " + e.getMessage());
                    e.printStackTrace();
                    alertes = new ArrayList<>();
                }
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            }

            // Récupérer les machines pour le formulaire
            List<com.maintenance.maintenance.model.entity.Machine> machines = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                try {
                    machines = machineService.listMachines(entrepriseId);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération des machines: " + e.getMessage());
                }
            }

            model.addAttribute("enterprises", enterprises);
            model.addAttribute("selectedEntrepriseId", entrepriseId != null ? entrepriseId : "");
            model.addAttribute("alertes", alertes);
            model.addAttribute("machines", machines);
            model.addAttribute("isSuperAdmin", isSuperAdmin(request));

            return "alertes/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des alertes: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Crée une nouvelle alerte
     */
    @PostMapping
    public String createAlerte(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                               @RequestParam("machineId") String machineId,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam("dateVerification") String dateVerificationStr,
                               @RequestParam(value = "activerRelance", required = false) String activerRelanceStr,
                               @RequestParam(value = "nombreRelances", required = false) String nombreRelancesStr,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut créer une alerte.");
            return "redirect:/alertes?entrepriseId=" + entrepriseId;
        }

        try {
            HttpSession session = request.getSession(true);
            if (!StringUtils.hasText(entrepriseId)) {
                entrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
            }

            if (!StringUtils.hasText(entrepriseId)) {
                redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une entreprise.");
                return "redirect:/alertes";
            }

            if (!StringUtils.hasText(machineId)) {
                redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une machine.");
                return "redirect:/alertes?entrepriseId=" + entrepriseId;
            }

            // Récupérer la machine pour obtenir son nom
            com.maintenance.maintenance.model.entity.Machine machine = machineService.getMachine(entrepriseId, machineId);
            if (machine == null) {
                redirectAttributes.addFlashAttribute("error", "Machine introuvable.");
                return "redirect:/alertes?entrepriseId=" + entrepriseId;
            }

            Alerte alerte = new Alerte();
            alerte.setEntrepriseId(entrepriseId);
            alerte.setMachineId(machineId);
            alerte.setMachineNom(machine.getNom());
            alerte.setDescription(description != null ? description : "");
            
            // Convertir la date du formulaire (YYYY-MM-DD) en timestamp
            try {
                LocalDate dateVerification = LocalDate.parse(dateVerificationStr);
                ZonedDateTime zonedDateTime = dateVerification.atStartOfDay(ZoneId.systemDefault());
                alerte.setDateVerification(zonedDateTime.toInstant().toEpochMilli());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Format de date invalide.");
                return "redirect:/alertes?entrepriseId=" + entrepriseId;
            }
            
            // Gérer les relances
            boolean activerRelance = "on".equals(activerRelanceStr) || "true".equals(activerRelanceStr);
            alerte.setActiverRelance(activerRelance);
            if (activerRelance) {
                try {
                    int nombreRelances = nombreRelancesStr != null && !nombreRelancesStr.isEmpty() ? 
                        Integer.parseInt(nombreRelancesStr) : 0;
                    alerte.setNombreRelances(nombreRelances);
                    alerte.setNombreRelancesEnvoyees(0);
                } catch (NumberFormatException e) {
                    alerte.setNombreRelances(0);
                    alerte.setNombreRelancesEnvoyees(0);
                }
            } else {
                alerte.setNombreRelances(0);
                alerte.setNombreRelancesEnvoyees(0);
            }
            alerte.setVerifie(false);
            
            // Récupérer les informations de l'utilisateur créateur
            String userId = (String) session.getAttribute("userId");
            alerte.setCreePar(userId);
            if (StringUtils.hasText(userId)) {
                try {
                    Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
                    if (user != null) {
                        String nom = (String) user.get("nom");
                        String prenom = (String) user.get("prenom");
                        alerte.setCreeParNom((prenom != null ? prenom + " " : "") + (nom != null ? nom : ""));
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de l'utilisateur créateur: " + e.getMessage());
                }
            }

            alertService.createAlerte(entrepriseId, alerte);
            redirectAttributes.addFlashAttribute("success", "Alerte créée avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création de l'alerte: " + e.getMessage());
        }

        return "redirect:/alertes?entrepriseId=" + entrepriseId;
    }

    /**
     * Met à jour une alerte
     */
    @PostMapping("/{entrepriseId}/{alerteId}")
    public String updateAlerte(@PathVariable String entrepriseId,
                              @PathVariable String alerteId,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam("dateVerification") String dateVerificationStr,
                              @RequestParam(value = "activerRelance", required = false) String activerRelanceStr,
                              @RequestParam(value = "nombreRelances", required = false) String nombreRelancesStr,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut modifier une alerte.");
            return "redirect:/alertes?entrepriseId=" + entrepriseId;
        }

        try {
            Alerte alerte = alertService.getAlerte(entrepriseId, alerteId);
            if (alerte == null) {
                redirectAttributes.addFlashAttribute("error", "Alerte introuvable.");
                return "redirect:/alertes?entrepriseId=" + entrepriseId;
            }

            if (description != null) {
                alerte.setDescription(description);
            }
            
            // Convertir la date du formulaire (YYYY-MM-DD) en timestamp
            try {
                LocalDate dateVerification = LocalDate.parse(dateVerificationStr);
                ZonedDateTime zonedDateTime = dateVerification.atStartOfDay(ZoneId.systemDefault());
                alerte.setDateVerification(zonedDateTime.toInstant().toEpochMilli());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Format de date invalide.");
                return "redirect:/alertes?entrepriseId=" + entrepriseId;
            }

            // Gérer les relances
            boolean activerRelance = "on".equals(activerRelanceStr) || "true".equals(activerRelanceStr);
            alerte.setActiverRelance(activerRelance);
            if (activerRelance) {
                try {
                    int nombreRelances = nombreRelancesStr != null && !nombreRelancesStr.isEmpty() ? 
                        Integer.parseInt(nombreRelancesStr) : 0;
                    alerte.setNombreRelances(nombreRelances);
                    if (alerte.getNombreRelancesEnvoyees() == null) {
                        alerte.setNombreRelancesEnvoyees(0);
                    }
                } catch (NumberFormatException e) {
                    alerte.setNombreRelances(0);
                }
            } else {
                alerte.setNombreRelances(0);
            }

            alertService.updateAlerte(entrepriseId, alerteId, alerte);
            redirectAttributes.addFlashAttribute("success", "Alerte mise à jour avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }

        return "redirect:/alertes?entrepriseId=" + entrepriseId;
    }

    /**
     * Supprime une alerte
     */
    @PostMapping("/{entrepriseId}/{alerteId}/delete")
    public String deleteAlerte(@PathVariable String entrepriseId,
                              @PathVariable String alerteId,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut supprimer une alerte.");
            return "redirect:/alertes?entrepriseId=" + entrepriseId;
        }

        try {
            alertService.deleteAlerte(entrepriseId, alerteId);
            redirectAttributes.addFlashAttribute("success", "Alerte supprimée avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }

        return "redirect:/alertes?entrepriseId=" + entrepriseId;
    }

    /**
     * Marque une alerte comme vérifiée et l'ajoute à l'historique
     */
    @PostMapping("/{entrepriseId}/{alerteId}/verifie")
    public String marquerVerifie(@PathVariable String entrepriseId,
                                 @PathVariable String alerteId,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut marquer une alerte comme vérifiée.");
            return "redirect:/alertes?entrepriseId=" + entrepriseId;
        }

        try {
            Alerte alerte = alertService.getAlerte(entrepriseId, alerteId);
            if (alerte == null) {
                redirectAttributes.addFlashAttribute("error", "Alerte introuvable.");
                return "redirect:/alertes?entrepriseId=" + entrepriseId;
            }

            // Marquer l'alerte comme vérifiée
            alerte.setVerifie(true);
            long now = System.currentTimeMillis();
            alerte.setDateVerificationReelle(now);
            alertService.updateAlerte(entrepriseId, alerteId, alerte);
            
            // Créer une entrée dans l'historique
            HistoriqueVerification historique = new HistoriqueVerification();
            historique.setEntrepriseId(entrepriseId);
            historique.setMachineId(alerte.getMachineId());
            historique.setMachineNom(alerte.getMachineNom());
            historique.setDescription(alerte.getDescription());
            historique.setDateVerificationProgrammee(alerte.getDateVerification());
            historique.setDateVerificationReelle(now);
            historique.setDateCreation(now);
            
            HttpSession session = request.getSession(false);
            if (session != null) {
                String userId = (String) session.getAttribute("userId");
                historique.setVerifiePar(userId);
                if (StringUtils.hasText(userId)) {
                    try {
                        Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
                        if (user != null) {
                            String nom = (String) user.get("nom");
                            String prenom = (String) user.get("prenom");
                            historique.setVerifieParNom((prenom != null ? prenom + " " : "") + (nom != null ? nom : ""));
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
                    }
                }
            }
            
            // Sauvegarder dans l'historique Firebase
            firebaseRealtimeService.createHistoriqueVerification(entrepriseId, historique);
            
            redirectAttributes.addFlashAttribute("success", "Alerte marquée comme vérifiée et ajoutée à l'historique.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }

        return "redirect:/alertes?entrepriseId=" + entrepriseId;
    }

    /**
     * API REST pour récupérer les alertes en JSON
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Alerte>> getAlertesApi(
            @RequestParam(value = "entrepriseId", required = false) String entrepriseId,
            HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(401).body(new ArrayList<>());
            }
            
            Boolean authenticated = (Boolean) session.getAttribute("authenticated");
            if (authenticated == null || !authenticated) {
                return ResponseEntity.status(401).body(new ArrayList<>());
            }

            if (!StringUtils.hasText(entrepriseId)) {
                entrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
            }

            List<Alerte> alertes = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                alertes = alertService.listAlertes(entrepriseId);
                if (alertes == null) {
                    alertes = new ArrayList<>();
                }
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(alertes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
}

