package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.entity.Ticket;
import com.maintenance.maintenance.model.entity.Commentaire;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import com.maintenance.maintenance.service.MachineService;
import com.maintenance.maintenance.service.TicketService;
import com.maintenance.maintenance.service.LocalFileStorageService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private MachineService machineService;
    
    @Autowired
    private LocalFileStorageService localFileStorageService;

    /**
     * Vérifie si l'utilisateur est connecté
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
     * Vérifie si l'utilisateur est super administrateur
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
     * Liste tous les tickets avec le système Kanban
     */
    @GetMapping
    public String listTickets(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                             Model model,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            HttpSession session = request.getSession(true);
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

            List<Ticket> allTickets = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                try {
                    allTickets = ticketService.listTickets(entrepriseId);
                    if (allTickets == null) {
                        allTickets = new ArrayList<>();
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération des tickets: " + e.getMessage());
                    e.printStackTrace();
                    allTickets = new ArrayList<>();
                }
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            }

            // Séparer les tickets par statut
            List<Ticket> ticketsAFaire = allTickets.stream()
                .filter(t -> "a_faire".equals(t.getStatut()))
                .collect(Collectors.toList());
            
            List<Ticket> ticketsEnCours = allTickets.stream()
                .filter(t -> "en_cours".equals(t.getStatut()))
                .collect(Collectors.toList());
            
            List<Ticket> ticketsTermines = allTickets.stream()
                .filter(t -> "termine".equals(t.getStatut()))
                .collect(Collectors.toList());
            
            List<Ticket> ticketsArchives = allTickets.stream()
                .filter(t -> "archive".equals(t.getStatut()))
                .collect(Collectors.toList());

            // Récupérer les utilisateurs pour les assignations
            List<Map<String, Object>> users = new ArrayList<>();
            try {
                users = firebaseRealtimeService.getAllUsers();
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération des utilisateurs: " + e.getMessage());
            }

            // Récupérer les machines pour les assignations
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
            model.addAttribute("ticketsAFaire", ticketsAFaire);
            model.addAttribute("ticketsEnCours", ticketsEnCours);
            model.addAttribute("ticketsTermines", ticketsTermines);
            model.addAttribute("ticketsArchives", ticketsArchives);
            model.addAttribute("users", users);
            model.addAttribute("machines", machines);
            model.addAttribute("isSuperAdmin", isSuperAdmin(request));
            
            // Ajouter l'ID de l'utilisateur actuel pour le chat
            String currentUserId = (String) session.getAttribute("userId");
            model.addAttribute("currentUserId", currentUserId != null ? currentUserId : "");

            return "tickets/list";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des tickets: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Crée un nouveau ticket
     */
    @PostMapping
    public String createTicket(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                              @RequestParam("titre") String titre,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "priorite", required = false) String priorite,
                              @RequestParam(value = "machineId", required = false) String machineId,
                              @RequestParam(value = "assigneA", required = false) String assigneA,
                              @RequestParam(value = "categorie", required = false) String categorie,
                              @RequestParam(value = "dateEcheance", required = false) String dateEcheanceStr,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        // Permettre aux superadmins et techniciens de créer des tickets
        HttpSession sessionCheck = request.getSession(false);
        String role = sessionCheck != null ? (String) sessionCheck.getAttribute("role") : null;
        if (role == null || (!"superadmin".equalsIgnoreCase(role) && !"technicien".equalsIgnoreCase(role))) {
            redirectAttributes.addFlashAttribute("error", "Seuls les super administrateurs et les techniciens peuvent créer un ticket.");
            return "redirect:/tickets?entrepriseId=" + entrepriseId;
        }

        try {
            HttpSession session = request.getSession(true);
            if (!StringUtils.hasText(entrepriseId)) {
                entrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
            }

            if (!StringUtils.hasText(entrepriseId)) {
                redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une entreprise.");
                return "redirect:/tickets";
            }

            if (!StringUtils.hasText(titre)) {
                redirectAttributes.addFlashAttribute("error", "Le titre est obligatoire.");
                return "redirect:/tickets?entrepriseId=" + entrepriseId;
            }

            Ticket ticket = new Ticket();
            ticket.setEntrepriseId(entrepriseId);
            ticket.setTitre(titre);
            ticket.setDescription(description != null ? description : "");
            ticket.setStatut("a_faire");
            ticket.setPriorite(priorite != null ? priorite : "normale");
            ticket.setMachineId(machineId);
            ticket.setAssigneA(assigneA);
            ticket.setCategorie(categorie);
            
            // Gérer la date d'échéance
            if (StringUtils.hasText(dateEcheanceStr)) {
                try {
                    // Convertir la date du format HTML (YYYY-MM-DD) en timestamp
                    java.time.LocalDate dateEcheance = java.time.LocalDate.parse(dateEcheanceStr);
                    ticket.setDateEcheance(java.time.ZonedDateTime.of(dateEcheance, java.time.LocalTime.of(23, 59), java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                } catch (Exception e) {
                    System.err.println("Erreur lors de la conversion de la date d'échéance: " + e.getMessage());
                }
            }
            
            // Récupérer les noms pour l'affichage
            if (StringUtils.hasText(machineId)) {
                try {
                    com.maintenance.maintenance.model.entity.Machine machine = machineService.getMachine(entrepriseId, machineId);
                    if (machine != null) {
                        ticket.setMachineNom(machine.getNom());
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de la machine: " + e.getMessage());
                }
            }
            
            if (StringUtils.hasText(assigneA)) {
                try {
                    Map<String, Object> user = firebaseRealtimeService.getUserById(assigneA);
                    if (user != null) {
                        String nom = (String) user.get("nom");
                        String prenom = (String) user.get("prenom");
                        ticket.setAssigneANom((prenom != null ? prenom + " " : "") + (nom != null ? nom : ""));
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
                }
            }
            
            String userId = (String) session.getAttribute("userId");
            ticket.setCreePar(userId);
            if (StringUtils.hasText(userId)) {
                try {
                    Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
                    if (user != null) {
                        String nom = (String) user.get("nom");
                        String prenom = (String) user.get("prenom");
                        ticket.setCreeParNom((prenom != null ? prenom + " " : "") + (nom != null ? nom : ""));
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de l'utilisateur créateur: " + e.getMessage());
                }
            }

            ticketService.createTicket(entrepriseId, ticket);
            redirectAttributes.addFlashAttribute("success", "Ticket créé avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création du ticket: " + e.getMessage());
        }

        return "redirect:/tickets?entrepriseId=" + entrepriseId;
    }

    /**
     * Met à jour le statut d'un ticket
     */
    @PostMapping("/{entrepriseId}/{ticketId}/statut")
    public String updateStatut(@PathVariable String entrepriseId,
                              @PathVariable String ticketId,
                              @RequestParam("statut") String statut,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = ticketService.getTicket(entrepriseId, ticketId);
            if (ticket == null) {
                redirectAttributes.addFlashAttribute("error", "Ticket introuvable.");
                return "redirect:/tickets?entrepriseId=" + entrepriseId;
            }

            ticket.setStatut(statut);
            ticketService.updateTicket(entrepriseId, ticketId, ticket);
            
            redirectAttributes.addFlashAttribute("success", "Statut du ticket mis à jour.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }

        return "redirect:/tickets?entrepriseId=" + entrepriseId;
    }

    /**
     * Met à jour un ticket
     */
    @PostMapping("/{entrepriseId}/{ticketId}")
    public String updateTicket(@PathVariable String entrepriseId,
                               @PathVariable String ticketId,
                               @RequestParam(value = "titre", required = false) String titre,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "priorite", required = false) String priorite,
                               @RequestParam(value = "machineId", required = false) String machineId,
                               @RequestParam(value = "assigneA", required = false) String assigneA,
                               @RequestParam(value = "categorie", required = false) String categorie,
                               @RequestParam(value = "dateEcheance", required = false) String dateEcheanceStr,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut modifier un ticket.");
            return "redirect:/tickets?entrepriseId=" + entrepriseId;
        }

        try {
            Ticket ticket = ticketService.getTicket(entrepriseId, ticketId);
            if (ticket == null) {
                redirectAttributes.addFlashAttribute("error", "Ticket introuvable.");
                return "redirect:/tickets?entrepriseId=" + entrepriseId;
            }

            if (StringUtils.hasText(titre)) {
                ticket.setTitre(titre);
            }
            if (description != null) {
                ticket.setDescription(description);
            }
            if (StringUtils.hasText(priorite)) {
                ticket.setPriorite(priorite);
            }
            if (machineId != null) {
                ticket.setMachineId(machineId);
                if (StringUtils.hasText(machineId)) {
                    try {
                        com.maintenance.maintenance.model.entity.Machine machine = machineService.getMachine(entrepriseId, machineId);
                        if (machine != null) {
                            ticket.setMachineNom(machine.getNom());
                        } else {
                            ticket.setMachineNom(null);
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la récupération de la machine: " + e.getMessage());
                    }
                } else {
                    ticket.setMachineNom(null);
                }
            }
            if (assigneA != null) {
                ticket.setAssigneA(assigneA);
                if (StringUtils.hasText(assigneA)) {
                    try {
                        Map<String, Object> user = firebaseRealtimeService.getUserById(assigneA);
                        if (user != null) {
                            String nom = (String) user.get("nom");
                            String prenom = (String) user.get("prenom");
                            ticket.setAssigneANom((prenom != null ? prenom + " " : "") + (nom != null ? nom : ""));
                        } else {
                            ticket.setAssigneANom(null);
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
                    }
                } else {
                    ticket.setAssigneANom(null);
                }
            }
            if (categorie != null) {
                ticket.setCategorie(categorie);
            }
            if (dateEcheanceStr != null) {
                if (StringUtils.hasText(dateEcheanceStr)) {
                    try {
                        java.time.LocalDate dateEcheance = java.time.LocalDate.parse(dateEcheanceStr);
                        ticket.setDateEcheance(java.time.ZonedDateTime.of(dateEcheance, java.time.LocalTime.of(23, 59), java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la conversion de la date d'échéance: " + e.getMessage());
                    }
                } else {
                    ticket.setDateEcheance(null);
                }
            }

            ticketService.updateTicket(entrepriseId, ticketId, ticket);
            redirectAttributes.addFlashAttribute("success", "Ticket mis à jour avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }

        return "redirect:/tickets?entrepriseId=" + entrepriseId;
    }

    /**
     * Archive un ticket
     */
    @PostMapping("/{entrepriseId}/{ticketId}/archive")
    public String archiveTicket(@PathVariable String entrepriseId,
                                @PathVariable String ticketId,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = ticketService.getTicket(entrepriseId, ticketId);
            if (ticket == null) {
                redirectAttributes.addFlashAttribute("error", "Ticket introuvable.");
                return "redirect:/tickets?entrepriseId=" + entrepriseId;
            }

            ticket.setStatut("archive");
            ticketService.updateTicket(entrepriseId, ticketId, ticket);
            
            redirectAttributes.addFlashAttribute("success", "Ticket archivé avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'archivage: " + e.getMessage());
        }

        return "redirect:/tickets?entrepriseId=" + entrepriseId;
    }

    /**
     * Supprime un ticket
     */
    @PostMapping("/{entrepriseId}/{ticketId}/delete")
    public String deleteTicket(@PathVariable String entrepriseId,
                               @PathVariable String ticketId,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut supprimer un ticket.");
            return "redirect:/tickets?entrepriseId=" + entrepriseId;
        }

        try {
            ticketService.deleteTicket(entrepriseId, ticketId);
            redirectAttributes.addFlashAttribute("success", "Ticket supprimé avec succès.");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }

        return "redirect:/tickets?entrepriseId=" + entrepriseId;
    }
    
    /**
     * API pour récupérer un ticket (JSON)
     */
    @GetMapping("/api/{entrepriseId}/{ticketId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTicketApi(@PathVariable String entrepriseId,
                                                             @PathVariable String ticketId,
                                                             HttpServletRequest request) {
        // Vérifier l'authentification
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(401)
                .header("Content-Type", "application/json")
                .body(Map.of("error", "Non authentifié"));
        }
        
        try {
            Ticket ticket = ticketService.getTicket(entrepriseId, ticketId);
            if (ticket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/json")
                    .body(Map.of("error", "Ticket introuvable"));
            }
            
            // Convertir le ticket en Map pour la sérialisation JSON
            Map<String, Object> ticketMap = new java.util.HashMap<>();
            ticketMap.put("ticketId", ticket.getTicketId());
            ticketMap.put("entrepriseId", ticket.getEntrepriseId());
            ticketMap.put("titre", ticket.getTitre());
            ticketMap.put("description", ticket.getDescription());
            ticketMap.put("statut", ticket.getStatut());
            ticketMap.put("priorite", ticket.getPriorite());
            ticketMap.put("machineId", ticket.getMachineId());
            ticketMap.put("machineNom", ticket.getMachineNom());
            ticketMap.put("assigneA", ticket.getAssigneA());
            ticketMap.put("assigneANom", ticket.getAssigneANom());
            ticketMap.put("creePar", ticket.getCreePar());
            ticketMap.put("creeParNom", ticket.getCreeParNom());
            ticketMap.put("dateCreation", ticket.getDateCreation());
            ticketMap.put("dateModification", ticket.getDateModification());
            ticketMap.put("dateTerminaison", ticket.getDateTerminaison());
            ticketMap.put("dateArchivage", ticket.getDateArchivage());
            ticketMap.put("dateEcheance", ticket.getDateEcheance());
            ticketMap.put("categorie", ticket.getCategorie());
            
            // Convertir les commentaires en List<Map>
            List<Map<String, Object>> commentairesList = new ArrayList<>();
            if (ticket.getCommentaires() != null) {
                for (com.maintenance.maintenance.model.entity.Commentaire commentaire : ticket.getCommentaires()) {
                    Map<String, Object> commentMap = new java.util.HashMap<>();
                    commentMap.put("texte", commentaire.getTexte());
                    commentMap.put("auteurId", commentaire.getAuteurId());
                    commentMap.put("auteurNom", commentaire.getAuteurNom());
                    commentMap.put("imageUrl", commentaire.getImageUrl());
                    commentMap.put("dateCreation", commentaire.getDateCreation());
                    commentairesList.add(commentMap);
                }
            }
            ticketMap.put("commentaires", commentairesList);
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(ticketMap);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "Erreur lors de la récupération du ticket: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body(errorMap);
        }
    }
    
    /**
     * API pour ajouter un commentaire (JSON)
     */
    @PostMapping("/api/{entrepriseId}/{ticketId}/commentaire")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addCommentaireApi(@PathVariable String entrepriseId,
                                                                  @PathVariable String ticketId,
                                                                  @RequestParam("texte") String texte,
                                                                  @RequestParam(value = "image", required = false) MultipartFile imageFile,
                                                                  HttpServletRequest request) {
        // Vérifier l'authentification
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return ResponseEntity.status(401)
                .header("Content-Type", "application/json")
                .body(Map.of("error", "Non authentifié"));
        }
        
        if (!StringUtils.hasText(texte) && (imageFile == null || imageFile.isEmpty())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body(Map.of("error", "Le commentaire ou une image est requis."));
        }
        
        try {
            Ticket ticket = ticketService.getTicket(entrepriseId, ticketId);
            if (ticket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/json")
                    .body(Map.of("error", "Ticket introuvable."));
            }
            
            String userId = (String) session.getAttribute("userId");
            String auteurNom = "Utilisateur inconnu";
            
            if (StringUtils.hasText(userId)) {
                try {
                    Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
                    if (user != null) {
                        String nom = (String) user.get("nom");
                        String prenom = (String) user.get("prenom");
                        auteurNom = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
                }
            }
            
            Commentaire commentaire = new Commentaire(StringUtils.hasText(texte) ? texte : "", userId, auteurNom);
            
            // Gérer l'upload d'image si présente
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imageUrl = localFileStorageService.saveCommentImage(entrepriseId, ticketId, imageFile);
                    commentaire.setImageUrl(imageUrl);
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'upload de l'image: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header("Content-Type", "application/json")
                        .body(Map.of("error", "Erreur lors de l'upload de l'image: " + e.getMessage()));
                }
            }
            
            ticket.addCommentaire(commentaire);
            ticketService.updateTicket(entrepriseId, ticketId, ticket);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Commentaire ajouté avec succès.");
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "Erreur lors de l'ajout du commentaire: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body(errorMap);
        }
    }
    
    /**
     * Ajoute un commentaire à un ticket (redirection pour compatibilité)
     */
    @PostMapping("/{entrepriseId}/{ticketId}/commentaire")
    public String addCommentaire(@PathVariable String entrepriseId,
                                 @PathVariable String ticketId,
                                 @RequestParam("texte") String texte,
                                 @RequestParam(value = "image", required = false) MultipartFile imageFile,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }
        
        if (!StringUtils.hasText(texte) && (imageFile == null || imageFile.isEmpty())) {
            redirectAttributes.addFlashAttribute("error", "Le commentaire ou une image est requis.");
            return "redirect:/tickets?entrepriseId=" + entrepriseId;
        }
        
        try {
            Ticket ticket = ticketService.getTicket(entrepriseId, ticketId);
            if (ticket == null) {
                redirectAttributes.addFlashAttribute("error", "Ticket introuvable.");
                return "redirect:/tickets?entrepriseId=" + entrepriseId;
            }
            
            HttpSession session = request.getSession(true);
            String userId = (String) session.getAttribute("userId");
            String auteurNom = "Utilisateur inconnu";
            
            if (StringUtils.hasText(userId)) {
                try {
                    Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
                    if (user != null) {
                        String nom = (String) user.get("nom");
                        String prenom = (String) user.get("prenom");
                        auteurNom = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
                }
            }
            
            Commentaire commentaire = new Commentaire(StringUtils.hasText(texte) ? texte : "", userId, auteurNom);
            
            // Gérer l'upload d'image si présente
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imageUrl = localFileStorageService.saveCommentImage(entrepriseId, ticketId, imageFile);
                    commentaire.setImageUrl(imageUrl);
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'upload de l'image: " + e.getMessage());
                    redirectAttributes.addFlashAttribute("error", "Erreur lors de l'upload de l'image: " + e.getMessage());
                }
            }
            
            ticket.addCommentaire(commentaire);
            ticketService.updateTicket(entrepriseId, ticketId, ticket);
            
            redirectAttributes.addFlashAttribute("success", "Commentaire ajouté avec succès.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'ajout du commentaire: " + e.getMessage());
        }
        
        return "redirect:/tickets?entrepriseId=" + entrepriseId;
    }
}

