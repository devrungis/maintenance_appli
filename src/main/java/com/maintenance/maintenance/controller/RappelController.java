package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.entity.Rappel;
import com.maintenance.maintenance.model.entity.HistoriqueVerification;
import com.maintenance.maintenance.service.RappelService;
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
@RequestMapping("/rappels")
public class RappelController {

    @Autowired
    private RappelService rappelService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private MachineService machineService;

    @Autowired
    private com.maintenance.maintenance.service.HistoriqueVerificationService historiqueVerificationService;

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
     * Liste toutes les rappels
     */
    @GetMapping
    public String listRappels(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
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

            // Récupérer le filtre (actif ou historique)
            String filtre = request.getParameter("filtre");
            if (filtre == null || filtre.isEmpty()) {
                filtre = "actif"; // Par défaut, afficher les rappels actifs
            }

            List<Rappel> rappels = new ArrayList<>();
            List<HistoriqueVerification> historiques = new ArrayList<>();
            
            if (StringUtils.hasText(entrepriseId)) {
                try {
                    List<Rappel> allRappels = rappelService.listRappels(entrepriseId);
                    if (allRappels != null) {
                        if ("historique".equals(filtre)) {
                            // Pour l'historique, récupérer les rappels vérifiés
                            for (Rappel rappel : allRappels) {
                                if (rappel.getVerifie() != null && rappel.getVerifie()) {
                                    rappels.add(rappel);
                                }
                            }
                            // Aussi récupérer l'historique des vérifications
                            try {
                                historiques = historiqueVerificationService.listHistoriqueVerifications(entrepriseId);
                                if (historiques == null) {
                                    historiques = new ArrayList<>();
                                }
                            } catch (Exception e) {
                                System.err.println("Erreur lors de la récupération de l'historique: " + e.getMessage());
                                historiques = new ArrayList<>();
                            }
                        } else {
                            // Pour les rappels actifs, filtrer les rappels vérifiés
                            for (Rappel rappel : allRappels) {
                                if (rappel.getVerifie() == null || !rappel.getVerifie()) {
                                    rappels.add(rappel);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération des rappels: " + e.getMessage());
                    e.printStackTrace();
                    rappels = new ArrayList<>();
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
            model.addAttribute("rappels", rappels);
            model.addAttribute("historiques", historiques);
            model.addAttribute("machines", machines);
            model.addAttribute("filtre", filtre);
            model.addAttribute("isSuperAdmin", isSuperAdmin(request));

            return "rappels/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des rappels: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Crée un nouveau rappel
     */
    @PostMapping
    public String createRappel(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
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
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut créer un rappel.");
            return "redirect:/rappels?entrepriseId=" + entrepriseId;
        }

        try {
            HttpSession session = request.getSession(true);
            if (!StringUtils.hasText(entrepriseId)) {
                entrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
            }

            if (!StringUtils.hasText(entrepriseId)) {
                redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une entreprise.");
                return "redirect:/rappels";
            }

            if (!StringUtils.hasText(machineId)) {
                redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une machine.");
                return "redirect:/rappels?entrepriseId=" + entrepriseId;
            }

            // Récupérer la machine pour obtenir son nom
            com.maintenance.maintenance.model.entity.Machine machine = machineService.getMachine(entrepriseId, machineId);
            if (machine == null) {
                redirectAttributes.addFlashAttribute("error", "Machine introuvable.");
                return "redirect:/rappels?entrepriseId=" + entrepriseId;
            }

            Rappel rappel = new Rappel();
            rappel.setEntrepriseId(entrepriseId);
            rappel.setMachineId(machineId);
            rappel.setMachineNom(machine.getNom());
            rappel.setDescription(description != null ? description : "");
            
            // Convertir la date du formulaire (YYYY-MM-DD) en timestamp
            try {
                LocalDate dateVerification = LocalDate.parse(dateVerificationStr);
                ZonedDateTime zonedDateTime = dateVerification.atStartOfDay(ZoneId.systemDefault());
                rappel.setDateVerification(zonedDateTime.toInstant().toEpochMilli());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Format de date invalide.");
                return "redirect:/rappels?entrepriseId=" + entrepriseId;
            }
            
            // Gérer les relances
            boolean activerRelance = "on".equals(activerRelanceStr) || "true".equals(activerRelanceStr);
            rappel.setActiverRelance(activerRelance);
            if (activerRelance) {
                try {
                    int nombreRelances = nombreRelancesStr != null && !nombreRelancesStr.isEmpty() ? 
                        Integer.parseInt(nombreRelancesStr) : 0;
                    rappel.setNombreRelances(nombreRelances);
                    rappel.setNombreRelancesEnvoyees(0);
                } catch (NumberFormatException e) {
                    rappel.setNombreRelances(0);
                    rappel.setNombreRelancesEnvoyees(0);
                }
            } else {
                rappel.setNombreRelances(0);
                rappel.setNombreRelancesEnvoyees(0);
            }
            rappel.setVerifie(false);
            
            // Récupérer les informations de l'utilisateur créateur
            String userId = (String) session.getAttribute("userId");
            rappel.setCreePar(userId);
            if (StringUtils.hasText(userId)) {
                try {
                    Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
                    if (user != null) {
                        String nom = (String) user.get("nom");
                        String prenom = (String) user.get("prenom");
                        rappel.setCreeParNom((prenom != null ? prenom + " " : "") + (nom != null ? nom : ""));
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de l'utilisateur créateur: " + e.getMessage());
                }
            }

            rappelService.createRappel(entrepriseId, rappel);
            redirectAttributes.addFlashAttribute("success", "Rappel créé avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création du rappel: " + e.getMessage());
        }

        return "redirect:/rappels?entrepriseId=" + entrepriseId;
    }

    /**
     * Met à jour un rappel
     */
    @PostMapping("/{entrepriseId}/{rappelId}")
    public String updateRappel(@PathVariable String entrepriseId,
                              @PathVariable String rappelId,
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
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut modifier un rappel.");
            return "redirect:/rappels?entrepriseId=" + entrepriseId;
        }

        try {
            Rappel rappel = rappelService.getRappel(entrepriseId, rappelId);
            if (rappel == null) {
                redirectAttributes.addFlashAttribute("error", "Rappel introuvable.");
                return "redirect:/rappels?entrepriseId=" + entrepriseId;
            }

            if (description != null) {
                rappel.setDescription(description);
            }
            
            // Convertir la date du formulaire (YYYY-MM-DD) en timestamp
            try {
                LocalDate dateVerification = LocalDate.parse(dateVerificationStr);
                ZonedDateTime zonedDateTime = dateVerification.atStartOfDay(ZoneId.systemDefault());
                rappel.setDateVerification(zonedDateTime.toInstant().toEpochMilli());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Format de date invalide.");
                return "redirect:/rappels?entrepriseId=" + entrepriseId;
            }

            // Gérer les relances
            boolean activerRelance = "on".equals(activerRelanceStr) || "true".equals(activerRelanceStr);
            rappel.setActiverRelance(activerRelance);
            if (activerRelance) {
                try {
                    int nombreRelances = nombreRelancesStr != null && !nombreRelancesStr.isEmpty() ? 
                        Integer.parseInt(nombreRelancesStr) : 0;
                    rappel.setNombreRelances(nombreRelances);
                    if (rappel.getNombreRelancesEnvoyees() == null) {
                        rappel.setNombreRelancesEnvoyees(0);
                    }
                } catch (NumberFormatException e) {
                    rappel.setNombreRelances(0);
                }
            } else {
                rappel.setNombreRelances(0);
            }

            rappelService.updateRappel(entrepriseId, rappelId, rappel);
            redirectAttributes.addFlashAttribute("success", "Rappel mis à jour avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }

        return "redirect:/rappels?entrepriseId=" + entrepriseId;
    }

    /**
     * Supprime un rappel
     */
    @PostMapping("/{entrepriseId}/{rappelId}/delete")
    public String deleteRappel(@PathVariable String entrepriseId,
                              @PathVariable String rappelId,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut supprimer un rappel.");
            return "redirect:/rappels?entrepriseId=" + entrepriseId;
        }

        try {
            rappelService.deleteRappel(entrepriseId, rappelId);
            redirectAttributes.addFlashAttribute("success", "Rappel supprimé avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }

        return "redirect:/rappels?entrepriseId=" + entrepriseId;
    }

    /**
     * Marque un rappel comme vérifié et l'ajoute à l'historique
     */
    @PostMapping("/{entrepriseId}/{rappelId}/verifie")
    public String marquerVerifie(@PathVariable String entrepriseId,
                                 @PathVariable String rappelId,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut marquer un rappel comme vérifié.");
            return "redirect:/rappels?entrepriseId=" + entrepriseId;
        }

        try {
            Rappel rappel = rappelService.getRappel(entrepriseId, rappelId);
            if (rappel == null) {
                redirectAttributes.addFlashAttribute("error", "Rappel introuvable.");
                return "redirect:/rappels?entrepriseId=" + entrepriseId;
            }

            // Marquer le rappel comme vérifié
            rappel.setVerifie(true);
            long now = System.currentTimeMillis();
            rappel.setDateVerificationReelle(now);
            rappelService.updateRappel(entrepriseId, rappelId, rappel);
            
            // Créer une entrée dans l'historique
            HistoriqueVerification historique = new HistoriqueVerification();
            historique.setEntrepriseId(entrepriseId);
            historique.setMachineId(rappel.getMachineId());
            historique.setMachineNom(rappel.getMachineNom());
            historique.setDescription(rappel.getDescription());
            historique.setDateVerificationProgrammee(rappel.getDateVerification());
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
            
            redirectAttributes.addFlashAttribute("success", "Rappel marqué comme vérifié et ajouté à l'historique.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }

        return "redirect:/rappels?entrepriseId=" + entrepriseId;
    }

    /**
     * API REST pour récupérer les rappels en JSON
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Rappel>> getRappelsApi(
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

            List<Rappel> rappels = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                rappels = rappelService.listRappels(entrepriseId);
                if (rappels == null) {
                    rappels = new ArrayList<>();
                }
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(rappels);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

}

