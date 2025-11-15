package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.dto.MachineForm;
import com.maintenance.maintenance.model.entity.Category;
import com.maintenance.maintenance.model.entity.Machine;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import com.maintenance.maintenance.service.MachineService;
import com.maintenance.maintenance.service.LocalFileStorageService;
import com.maintenance.maintenance.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/machines")
public class MachineController {

    @Autowired
    private MachineService machineService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private LocalFileStorageService localFileStorageService;

    @Autowired
    private CategoryService categoryService;

    /**
     * Vérifie si l'utilisateur est connecté (tous les rôles autorisés)
     */
    private boolean ensureAuthenticated(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // Toujours créer/obtenir une session
        HttpSession session = request.getSession(true);
        
        // Vérifier si l'utilisateur est authentifié via Spring Security
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null 
            && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
            && !"anonymousUser".equals(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString())) {
            // Restaurer/mettre à jour la session
            session.setAttribute("authenticated", true);
            return true;
        }
        
        // Vérifier si la session contient déjà l'authentification
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated != null && authenticated) {
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

    @GetMapping
    public String listMachines(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                               @RequestParam(value = "categoryId", required = false) Long categoryId,
                               @RequestParam(value = "operationnel", required = false) String operationnel,
                               @RequestParam(value = "estSecours", required = false) String estSecours,
                               @RequestParam(value = "search", required = false) String search,
                               Model model,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            // Préserver la session
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            
            List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
            if (enterprises == null) {
                enterprises = new ArrayList<>();
            }

            // Récupérer depuis la session si pas fourni dans la requête
            if (!StringUtils.hasText(entrepriseId)) {
                String lastEntrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
                if (StringUtils.hasText(lastEntrepriseId)) {
                    entrepriseId = lastEntrepriseId;
                } else if (!enterprises.isEmpty()) {
                    // Prendre la première entreprise par défaut
                entrepriseId = enterprises.get(0).get("entrepriseId").toString();
                }
            }

            List<Machine> allMachines = new ArrayList<>();
            List<Machine> machines = new ArrayList<>();
            
            if (StringUtils.hasText(entrepriseId)) {
                allMachines = machineService.listMachines(entrepriseId);
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
                
                // Appliquer tous les filtres en une seule passe
                machines = allMachines.stream()
                    .filter(m -> {
                        if (m == null) return false;
                        
                        // Filtre : exclure les machines de secours qui sont déjà rattachées à une machine principale
                        // (sauf si on filtre spécifiquement pour les secours)
                        if (estSecours == null || estSecours.isEmpty() || !"true".equalsIgnoreCase(estSecours)) {
                            // Si c'est une machine de secours rattachée, l'exclure
                            if (m.getEstMachineSecours() != null && m.getEstMachineSecours() 
                                && m.getMachinePrincipaleId() != null && !m.getMachinePrincipaleId().isEmpty()) {
                                return false;
                            }
                        }
                        
                        // Filtre par recherche (nom, numéro de série, IP, emplacement, catégorie)
                        if (StringUtils.hasText(search)) {
                            String searchLower = search.toLowerCase();
                            boolean matchesSearch = false;
                            
                            if (m.getNom() != null && m.getNom().toLowerCase().contains(searchLower)) {
                                matchesSearch = true;
                            } else if (m.getNumeroSerie() != null && m.getNumeroSerie().toLowerCase().contains(searchLower)) {
                                matchesSearch = true;
                            } else if (m.getAdresseIP() != null && m.getAdresseIP().toLowerCase().contains(searchLower)) {
                                matchesSearch = true;
                            } else if (m.getEmplacement() != null && m.getEmplacement().toLowerCase().contains(searchLower)) {
                                matchesSearch = true;
                            } else if (m.getCategoryName() != null && m.getCategoryName().toLowerCase().contains(searchLower)) {
                                matchesSearch = true;
                            } else if (m.getNotes() != null && m.getNotes().toLowerCase().contains(searchLower)) {
                                matchesSearch = true;
                            }
                            
                            if (!matchesSearch) {
                                return false;
                            }
                        }
                        
                        // Filtre par catégorie
                        if (categoryId != null) {
                            if (m.getCategoryId() == null || !m.getCategoryId().equals(categoryId)) {
                                return false;
                            }
                        }
                        
                        // Filtre par statut opérationnel
                        if (StringUtils.hasText(operationnel)) {
                            boolean isOperationnel = "true".equalsIgnoreCase(operationnel);
                            Boolean machineOperationnel = m.getOperationnel();
                            
                            if (isOperationnel) {
                                // Filtrer pour les machines opérationnelles
                                // Une machine est opérationnelle si operationnel == true ET enReparation != true
                                if (machineOperationnel == null || !machineOperationnel || 
                                    (m.getEnReparation() != null && m.getEnReparation())) {
                                    return false;
                                }
                            } else {
                                // Filtrer pour les machines non opérationnelles
                                // Une machine est non opérationnelle si operationnel == false OU enReparation == true
                                if (machineOperationnel != null && machineOperationnel && 
                                    (m.getEnReparation() == null || !m.getEnReparation())) {
                                    return false;
                                }
                            }
                        }
                        
                        // Filtre par type (secours/principale)
                        if (StringUtils.hasText(estSecours)) {
                            Boolean machineEstSecours = m.getEstMachineSecours();
                            Boolean machineEstEntrepot = m.getEstMachineEntrepot() != null ? m.getEstMachineEntrepot() : false;
                            
                            if ("entrepot".equals(estSecours)) {
                                // Filtrer pour les machines entrepôt
                                if (!machineEstEntrepot) {
                                    return false;
                                }
                            } else if ("true".equalsIgnoreCase(estSecours)) {
                                // Filtrer pour les machines de secours (même celles attachées, mais pas entrepôt)
                                if (machineEstEntrepot || machineEstSecours == null || !machineEstSecours) {
                                    return false;
                                }
                            } else {
                                // Filtrer pour les machines principales (pas secours, pas entrepôt)
                                if (machineEstEntrepot || (machineEstSecours != null && machineEstSecours)) {
                                    return false;
                                }
                            }
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
            }
            
            // Récupérer toutes les catégories pour le filtre
            List<Category> categories = categoryService.findAll();

            // Créer une map pour compter rapidement les machines de secours par machine principale
            Map<String, Long> secoursCountMap = new HashMap<>();
            // Map pour stocker les machines principales non fonctionnelles avec leurs machines de secours disponibles
            // Clé: machineId, Valeur: Map contenant "machine" (Machine principale) et "secours" (List<Machine>)
            Map<String, Map<String, Object>> machinesPrincipalesNonFonctionnelles = new HashMap<>();
            
            if (StringUtils.hasText(entrepriseId) && allMachines != null) {
                for (Machine m : allMachines) {
                    if (m != null && m.getEstMachineSecours() != null && m.getEstMachineSecours() 
                        && m.getMachinePrincipaleId() != null && !m.getMachinePrincipaleId().isEmpty()
                        && m.getMachineId() != null) {
                        String principaleId = m.getMachinePrincipaleId();
                        secoursCountMap.put(principaleId, 
                            secoursCountMap.getOrDefault(principaleId, 0L) + 1);
                    }
                }
                
                // Détecter les machines principales non fonctionnelles avec des machines de secours disponibles
                for (Machine machine : allMachines) {
                    if (machine != null && machine.getMachineId() != null) {
                        if (machine.getEstMachineEntrepot() != null && machine.getEstMachineEntrepot()) {
                            continue;
                        }
                        // Vérifier si c'est une machine principale
                        boolean isMachinePrincipale = machine.getEstMachineSecours() == null || !machine.getEstMachineSecours();
                        
                        if (isMachinePrincipale) {
                            // Vérifier si elle est non fonctionnelle
                            boolean isNonFonctionnelle = machine.getOperationnel() == null || !machine.getOperationnel();
                            
                            if (isNonFonctionnelle) {
                                // Chercher les machines de secours fonctionnelles attachées à cette machine principale
                                List<Machine> secoursDisponibles = allMachines.stream()
                                    .filter(m -> m.getEstMachineSecours() != null && m.getEstMachineSecours())
                                    .filter(m -> machine.getMachineId().equals(m.getMachinePrincipaleId()))
                                    .filter(m -> m.getEnReparation() == null || !m.getEnReparation()) // Exclure celles en réparation
                                    .filter(m -> m.getOperationnel() != null && m.getOperationnel()) // Seulement les fonctionnelles
                                    .collect(Collectors.toList());
                                
                                // Si des machines de secours sont disponibles, ajouter à la map
                                if (!secoursDisponibles.isEmpty()) {
                                    Map<String, Object> info = new HashMap<>();
                                    info.put("machine", machine);
                                    info.put("secours", secoursDisponibles);
                                    machinesPrincipalesNonFonctionnelles.put(machine.getMachineId(), info);
                                }
                            }
                        }
                    }
                }
            }

            model.addAttribute("enterprises", enterprises);
            model.addAttribute("selectedEntrepriseId", entrepriseId != null ? entrepriseId : "");
            model.addAttribute("machines", machines != null ? machines : new ArrayList<>());
            model.addAttribute("secoursCountMap", secoursCountMap != null ? secoursCountMap : new HashMap<>());
            model.addAttribute("machinesPrincipalesNonFonctionnelles", machinesPrincipalesNonFonctionnelles);
            model.addAttribute("isSuperAdmin", isSuperAdmin(request));
            model.addAttribute("categories", categories != null ? categories : new ArrayList<>());
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("selectedOperationnel", operationnel);
            model.addAttribute("selectedEstSecours", estSecours);
            model.addAttribute("searchQuery", search != null ? search : "");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des machines: " + e.getMessage());
            return "redirect:/dashboard";
        }

        return "machines/list";
    }

    @GetMapping("/{entrepriseId}/{machineId}/attach-secours")
    public String showAttachSecoursForm(@PathVariable String entrepriseId,
                                        @PathVariable String machineId,
                                        Model model,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Machine machine = machineService.getMachine(entrepriseId, machineId);
            if (machine == null) {
                redirectAttributes.addFlashAttribute("error", "Machine introuvable.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }
            
            // Vérifier si la machine est une machine principale ou une machine de secours
            boolean isMachinePrincipale = machine.getEstMachineSecours() == null || !machine.getEstMachineSecours();
            
            if (isMachinePrincipale) {
                // Si c'est une machine principale, récupérer les machines de secours disponibles (non rattachées)
                List<Machine> allMachines = machineService.listMachines(entrepriseId);
                List<Machine> machinesSecoursDisponibles = allMachines.stream()
                    .filter(m -> m.getEstMachineSecours() != null && m.getEstMachineSecours())
                    .filter(m -> m.getMachinePrincipaleId() == null || m.getMachinePrincipaleId().isEmpty())
                    .filter(m -> m.getEnReparation() == null || !m.getEnReparation()) // Exclure les machines en réparation
                    .filter(m -> !m.getMachineId().equals(machineId)) // Exclure la machine courante
                    .collect(Collectors.toList());
                
                // Récupérer les machines de secours déjà rattachées à cette machine principale
                List<Machine> machinesSecoursRattachees = allMachines.stream()
                    .filter(m -> m.getEstMachineSecours() != null && m.getEstMachineSecours())
                    .filter(m -> m.getMachinePrincipaleId() != null && m.getMachinePrincipaleId().equals(machineId))
                    .collect(Collectors.toList());
                
                model.addAttribute("machinesSecoursDisponibles", machinesSecoursDisponibles);
                model.addAttribute("machinesSecoursRattachees", machinesSecoursRattachees);
            } else {
                // Si c'est une machine de secours, récupérer les machines principales pour changer le rattachement
                List<Machine> allMachines = machineService.listMachines(entrepriseId);
                List<Machine> machinesPrincipales = allMachines.stream()
                    .filter(m -> m.getEstMachineSecours() == null || !m.getEstMachineSecours())
                    .filter(m -> !m.getMachineId().equals(machineId)) // Exclure la machine courante
                    .collect(Collectors.toList());
                
                String currentMainMachineId = machine.getMachinePrincipaleId();
                model.addAttribute("machinesPrincipales", machinesPrincipales);
                model.addAttribute("currentMainMachineId", currentMainMachineId);
            }
            
            model.addAttribute("machine", machine);
            model.addAttribute("entrepriseId", entrepriseId);
            model.addAttribute("isSuperAdmin", isSuperAdmin(request));
            model.addAttribute("isMachinePrincipale", isMachinePrincipale);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement: " + e.getMessage());
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        }

        return "machines/attach-secours";
    }

    @GetMapping("/{entrepriseId}/{machineId}/secours")
    public String showSecoursDetails(@PathVariable String entrepriseId,
                                     @PathVariable String machineId,
                                     Model model,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            // Nettoyer l'entrepriseId si plusieurs valeurs sont présentes
            if (StringUtils.hasText(entrepriseId) && entrepriseId.contains(",")) {
                entrepriseId = entrepriseId.split(",")[0].trim();
            }
            
            // Préserver la session
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            
            Machine machinePrincipale = machineService.getMachine(entrepriseId, machineId);
            if (machinePrincipale == null) {
                redirectAttributes.addFlashAttribute("error", "Machine principale introuvable.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }
            
            // Vérifier que c'est bien une machine principale
            if (machinePrincipale.getEstMachineSecours() != null && machinePrincipale.getEstMachineSecours()) {
                redirectAttributes.addFlashAttribute("error", "Cette machine n'est pas une machine principale.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }
            
            // Récupérer toutes les machines de secours rattachées à cette machine principale
            List<Machine> allMachines = machineService.listMachines(entrepriseId);
            if (allMachines == null) {
                allMachines = new ArrayList<>();
            }
            
            List<Machine> machinesSecoursRattachees = allMachines.stream()
                .filter(m -> m != null)
                .filter(m -> m.getEstMachineSecours() != null && m.getEstMachineSecours())
                .filter(m -> m.getMachinePrincipaleId() != null && m.getMachinePrincipaleId().equals(machineId))
                .collect(Collectors.toList());
            
            // S'assurer que la liste n'est jamais null
            if (machinesSecoursRattachees == null) {
                machinesSecoursRattachees = new ArrayList<>();
            }
            
            model.addAttribute("machinePrincipale", machinePrincipale);
            model.addAttribute("machinesSecoursRattachees", machinesSecoursRattachees);
            model.addAttribute("entrepriseId", entrepriseId);
            model.addAttribute("isSuperAdmin", isSuperAdmin(request));
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement: " + e.getMessage());
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        }

        return "machines/secours-details";
    }

    @PostMapping("/{entrepriseId}/{machineId}/detach-secours")
    public String detachSecours(@PathVariable String entrepriseId,
                                @PathVariable String machineId,
                                @RequestParam("secoursId") String secoursId,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Machine machineSecours = machineService.getMachine(entrepriseId, secoursId);
            if (machineSecours == null) {
                redirectAttributes.addFlashAttribute("error", "Machine de secours introuvable.");
                return "redirect:/machines/" + entrepriseId + "/" + machineId + "/secours";
            }
            
            // Détacher la machine de secours : elle reste une machine de secours non rattachée
            machineSecours.setMachinePrincipaleId(null);
            machineSecours.setEstMachineSecours(true);
            machineService.updateMachine(entrepriseId, secoursId, machineSecours);
            
            redirectAttributes.addFlashAttribute("success", "Machine de secours détachée avec succès.");
            
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/secours";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du détachement: " + e.getMessage());
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/secours";
        }
    }

    @PostMapping("/{entrepriseId}/{machineId}/attach-secours")
    public String attachSecours(@PathVariable String entrepriseId,
                                @PathVariable String machineId,
                                @RequestParam(value = "action", required = false) String action,
                                @RequestParam(value = "machinePrincipaleId", required = false) String machinePrincipaleId,
                                @RequestParam(value = "machinesSecoursIds", required = false) String[] machinesSecoursIds,
                                @RequestParam(value = "createNew", required = false) String createNew,
                                @RequestParam(value = "detach", required = false) String detach,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Machine machine = machineService.getMachine(entrepriseId, machineId);
            if (machine == null) {
                redirectAttributes.addFlashAttribute("error", "Machine introuvable.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }

            if (Boolean.TRUE.equals(machine.getEstMachineEntrepot())) {
                redirectAttributes.addFlashAttribute("error", "Ce matériel neuf doit être configuré en machine principale ou de secours avant de pouvoir rattacher d'autres machines.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }
            
            // Si action = "detach", détacher la machine de secours
            if ("detach".equals(detach) || "true".equals(detach)) {
                machine.setMachinePrincipaleId(null);
                // La machine reste de secours, prête à être rattachée à une autre principale
                machine.setEstMachineSecours(true);
                machineService.updateMachine(entrepriseId, machineId, machine);
                redirectAttributes.addFlashAttribute("success", "Machine détachée avec succès.");
                
                HttpSession session = request.getSession(true);
                session.setAttribute("authenticated", true);
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }
            
            // Si action = "create", rediriger vers la création d'une nouvelle machine
            if ("create".equals(action) || "true".equals(createNew)) {
                redirectAttributes.addFlashAttribute("attachToMachineId", machineId);
                redirectAttributes.addFlashAttribute("info", "Créez une nouvelle machine qui sera automatiquement rattachée comme machine de secours.");
                return "redirect:/machines/create?entrepriseId=" + entrepriseId + "&attachTo=" + machineId;
            }
            
            // Vérifier si c'est une machine principale ou une machine de secours
            boolean isMachinePrincipale = machine.getEstMachineSecours() == null || !machine.getEstMachineSecours();
            
            if (isMachinePrincipale) {
                // Si c'est une machine principale, rattacher les machines de secours sélectionnées
                if (machinesSecoursIds == null || machinesSecoursIds.length == 0) {
                    redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner au moins une machine de secours.");
                    return "redirect:/machines/" + entrepriseId + "/" + machineId + "/attach-secours";
                }
                
                int count = 0;
                for (String secoursId : machinesSecoursIds) {
                    if (StringUtils.hasText(secoursId)) {
                        Machine machineSecours = machineService.getMachine(entrepriseId, secoursId);
                        if (machineSecours != null && machineSecours.getEstMachineSecours() != null && machineSecours.getEstMachineSecours()) {
                            machineSecours.setMachinePrincipaleId(machineId);
                            machineService.updateMachine(entrepriseId, secoursId, machineSecours);
                            count++;
                        }
                    }
                }
                
                redirectAttributes.addFlashAttribute("success", count + " machine(s) de secours rattachée(s) avec succès.");
            } else {
                // Si c'est une machine de secours, changer la machine principale associée
                if (!StringUtils.hasText(machinePrincipaleId)) {
                    redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une machine principale.");
                    return "redirect:/machines/" + entrepriseId + "/" + machineId + "/attach-secours";
                }
                
                // Vérifier que la machine principale existe
                Machine machinePrincipale = machineService.getMachine(entrepriseId, machinePrincipaleId);
                if (machinePrincipale == null) {
                    redirectAttributes.addFlashAttribute("error", "La machine principale sélectionnée n'existe pas.");
                    return "redirect:/machines/" + entrepriseId + "/" + machineId + "/attach-secours";
                }
                
                // Vérifier que la machine principale n'est pas déjà une machine de secours
                if (machinePrincipale.getEstMachineSecours() != null && machinePrincipale.getEstMachineSecours()) {
                    redirectAttributes.addFlashAttribute("error", "Une machine de secours ne peut pas être une machine principale.");
                    return "redirect:/machines/" + entrepriseId + "/" + machineId + "/attach-secours";
                }
                
                // Changer la machine principale associée
                machine.setMachinePrincipaleId(machinePrincipaleId);
                machineService.updateMachine(entrepriseId, machineId, machine);
                
                redirectAttributes.addFlashAttribute("success", "Machine de secours rattachée à la nouvelle machine principale avec succès.");
            }
            
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du rattachement: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/attach-secours";
        }

        return "redirect:/machines?entrepriseId=" + entrepriseId;
    }

    @GetMapping("/{entrepriseId}/{machineId}/promote-secours-form")
    public String showPromoteSecoursForm(@PathVariable String entrepriseId,
                                        @PathVariable String machineId,
                                        Model model,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Machine machinePrincipale = machineService.getMachine(entrepriseId, machineId);
            if (machinePrincipale == null) {
                redirectAttributes.addFlashAttribute("error", "Machine principale introuvable.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }

            // Vérifier que la machine principale est non fonctionnelle
            if (machinePrincipale.getOperationnel() != null && machinePrincipale.getOperationnel()) {
                redirectAttributes.addFlashAttribute("error", "Cette machine principale est fonctionnelle. Vous ne pouvez promouvoir une machine de secours que si la principale est non fonctionnelle.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }

            // Récupérer toutes les machines de secours rattachées à cette principale et fonctionnelles
            List<Machine> allMachines = machineService.listMachines(entrepriseId);
            List<Machine> machinesSecoursDisponibles = allMachines.stream()
                .filter(m -> m.getEstMachineSecours() != null && m.getEstMachineSecours())
                .filter(m -> m.getMachinePrincipaleId() != null && m.getMachinePrincipaleId().equals(machineId))
                .filter(m -> m.getEnReparation() == null || !m.getEnReparation()) // Exclure celles déjà en réparation
                .filter(m -> m.getOperationnel() != null && m.getOperationnel()) // Seulement les fonctionnelles
                .collect(Collectors.toList());

            if (machinesSecoursDisponibles.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Aucune machine de secours fonctionnelle disponible pour remplacer cette machine principale.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }

            model.addAttribute("machinePrincipale", machinePrincipale);
            model.addAttribute("machinesSecoursDisponibles", machinesSecoursDisponibles);
            model.addAttribute("entrepriseId", entrepriseId);
            model.addAttribute("isSuperAdmin", isSuperAdmin(request));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        }

        return "machines/promote-secours";
    }

    @PostMapping("/{entrepriseId}/{machineId}/promote-secours")
    public String promoteSecours(@PathVariable String entrepriseId,
                                 @PathVariable String machineId,
                                 @RequestParam(value = "nouvellePrincipaleId", required = true) String nouvellePrincipaleId,
                                 RedirectAttributes redirectAttributes,
                                 HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Machine machinePrincipale = machineService.getMachine(entrepriseId, machineId);
            if (machinePrincipale == null) {
                redirectAttributes.addFlashAttribute("error", "Machine principale introuvable.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }

            // Vérifier que la machine principale est non fonctionnelle
            if (machinePrincipale.getOperationnel() != null && machinePrincipale.getOperationnel()) {
                redirectAttributes.addFlashAttribute("error", "Cette machine principale est fonctionnelle. Vous ne pouvez promouvoir une machine de secours que si la principale est non fonctionnelle.");
                return "redirect:/machines/" + entrepriseId + "/" + machineId + "/promote-secours-form";
            }

            Machine nouvellePrincipale = machineService.getMachine(entrepriseId, nouvellePrincipaleId);
            if (nouvellePrincipale == null) {
                redirectAttributes.addFlashAttribute("error", "Machine de secours introuvable.");
                return "redirect:/machines/" + entrepriseId + "/" + machineId + "/promote-secours-form";
            }

            // Vérifier que c'est bien une machine de secours rattachée à cette principale
            if (nouvellePrincipale.getEstMachineSecours() == null || !nouvellePrincipale.getEstMachineSecours() 
                || !machineId.equals(nouvellePrincipale.getMachinePrincipaleId())) {
                redirectAttributes.addFlashAttribute("error", "La machine sélectionnée n'est pas une machine de secours rattachée à cette machine principale.");
                return "redirect:/machines/" + entrepriseId + "/" + machineId + "/promote-secours-form";
            }

            // Récupérer toutes les machines de secours rattachées à cette principale
            List<Machine> allMachines = machineService.listMachines(entrepriseId);
            List<Machine> machinesSecours = allMachines.stream()
                .filter(m -> m.getEstMachineSecours() != null && m.getEstMachineSecours())
                .filter(m -> machineId.equals(m.getMachinePrincipaleId()))
                .collect(Collectors.toList());

            // Promouvoir la machine sélectionnée en principale
            nouvellePrincipale.setEstMachineSecours(false);
            nouvellePrincipale.setMachinePrincipaleId(null);
            nouvellePrincipale.setOperationnel(true);
            nouvellePrincipale.setEnReparation(false);
            machineService.updateMachine(entrepriseId, nouvellePrincipaleId, nouvellePrincipale);

            // Mettre les autres machines de secours en réparation (non fonctionnelles)
            for (Machine secours : machinesSecours) {
                if (!secours.getMachineId().equals(nouvellePrincipaleId)) {
                    secours.setEnReparation(true);
                    secours.setOperationnel(false);
                    secours.setEstMachineSecours(false); // Plus une machine de secours
                    secours.setMachinePrincipaleId(null); // Plus rattachée
                    machineService.updateMachine(entrepriseId, secours.getMachineId(), secours);
                }
            }

            // Mettre l'ancienne principale en réparation
            machinePrincipale.setEnReparation(true);
            machinePrincipale.setOperationnel(false);
            machinePrincipale.setEstMachineSecours(false);
            machinePrincipale.setMachinePrincipaleId(null);
            machineService.updateMachine(entrepriseId, machineId, machinePrincipale);

            redirectAttributes.addFlashAttribute("success", "Machine de secours promue avec succès. Les autres machines sont passées en réparation.");

            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la promotion: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/promote-secours-form";
        }

        return "redirect:/machines?entrepriseId=" + entrepriseId;
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                                 @RequestParam(value = "attachTo", required = false) String attachToMachineId,
                                 Model model,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            // Nettoyer l'entrepriseId si plusieurs valeurs sont présentes (séparées par des virgules)
            if (StringUtils.hasText(entrepriseId) && entrepriseId.contains(",")) {
                entrepriseId = entrepriseId.split(",")[0].trim();
            }
            
            List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
            if (enterprises == null) {
                enterprises = new ArrayList<>();
            }
            
            // Si aucune entreprise n'est fournie, utiliser celle de la session ou la première disponible
            HttpSession session = request.getSession(true);
            if (!StringUtils.hasText(entrepriseId)) {
                String lastEntrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
                if (StringUtils.hasText(lastEntrepriseId)) {
                    entrepriseId = lastEntrepriseId;
                } else if (!enterprises.isEmpty()) {
                    entrepriseId = enterprises.get(0).get("entrepriseId").toString();
                    // Sauvegarder dans la session
                    session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
                }
            } else {
                // Sauvegarder dans la session pour les prochaines fois
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            }
            
            // Toujours définir selectedEntrepriseId, même si null
            model.addAttribute("enterprises", enterprises);
            model.addAttribute("selectedEntrepriseId", entrepriseId);
            System.out.println("=== MachineController.showCreateForm: selectedEntrepriseId = " + entrepriseId + " ===");
            
            // Si attachTo est fourni, charger la machine principale pour affichage
            Machine attachToMachine = null;
            if (StringUtils.hasText(attachToMachineId) && StringUtils.hasText(entrepriseId)) {
                try {
                    attachToMachine = machineService.getMachine(entrepriseId, attachToMachineId);
                    model.addAttribute("attachToMachine", attachToMachine);
                    model.addAttribute("attachToMachineId", attachToMachineId);
                } catch (Exception e) {
                    // Ignorer si la machine n'existe pas
                }
            }
            
            // Charger uniquement les machines principales (pas les machines de secours) pour permettre la sélection
            List<Machine> machinesPrincipales = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                List<Machine> allMachines = machineService.listMachines(entrepriseId);
                machinesPrincipales = allMachines.stream()
                    .filter(m -> m.getEstMachineSecours() == null || !m.getEstMachineSecours())
                    .collect(Collectors.toList());
            }
            model.addAttribute("machines", machinesPrincipales);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des entreprises: " + e.getMessage());
            return "redirect:/machines";
        }

        if (!model.containsAttribute("machineForm")) {
            MachineForm machineForm = new MachineForm();
            if (StringUtils.hasText(entrepriseId)) {
            machineForm.setEntrepriseId(entrepriseId);
            }
            // Si attachTo est fourni, pré-remplir comme machine de secours
            if (StringUtils.hasText(attachToMachineId)) {
                machineForm.setEstMachineSecours(true);
                machineForm.setMachinePrincipaleId(attachToMachineId);
            }
            model.addAttribute("machineForm", machineForm);
        } else {
            Object existing = model.asMap().get("machineForm");
            if (existing instanceof MachineForm form) {
                if (StringUtils.hasText(entrepriseId)) {
                form.setEntrepriseId(entrepriseId);
                }
            }
        }

        model.addAttribute("categories", categoryService.findAll());
        return "machines/create";
    }

    @PostMapping
    public String createMachine(@ModelAttribute("machineForm") MachineForm machineForm,
                                @RequestParam(value = "machineType", required = false) String machineTypeSelection,
                                @RequestParam(value = "photoFiles", required = false) MultipartFile[] photoFiles,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        // Récupérer l'entrepriseId depuis le formulaire, la session ou les paramètres
        String entrepriseId = machineForm.getEntrepriseId();
        
        // Nettoyer l'entrepriseId si plusieurs valeurs sont présentes (séparées par des virgules)
        if (StringUtils.hasText(entrepriseId) && entrepriseId.contains(",")) {
            entrepriseId = entrepriseId.split(",")[0].trim();
        }
        
        if (!StringUtils.hasText(entrepriseId)) {
            HttpSession session = request.getSession(true);
            entrepriseId = (String) session.getAttribute("lastSelectedEntrepriseId");
            if (!StringUtils.hasText(entrepriseId)) {
                try {
                    List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
                    if (enterprises != null && !enterprises.isEmpty()) {
                        entrepriseId = enterprises.get(0).get("entrepriseId").toString();
                    }
                } catch (Exception e) {
                    // Ignorer l'erreur, on redirigera vers le formulaire
                }
            }
        }
        
        if (!StringUtils.hasText(entrepriseId)) {
            redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une entreprise.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/create";
        }
        
        // Mettre à jour l'entrepriseId dans le formulaire (nettoyé)
        machineForm.setEntrepriseId(entrepriseId);
        if (!StringUtils.hasText(machineForm.getNom())) {
            redirectAttributes.addFlashAttribute("error", "Le nom de la machine est obligatoire.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/create?entrepriseId=" + machineForm.getEntrepriseId();
        }
        
        applyMachineTypeSelection(machineForm, machineTypeSelection);

        // Validation : au moins numéro de série OU adresse IP doit être renseigné SAUF pour les machines entrepôt
        Boolean estMachineEntrepot = machineForm.getEstMachineEntrepot() != null ? machineForm.getEstMachineEntrepot() : false;
        if (estMachineEntrepot && StringUtils.hasText(machineForm.getAdresseIP())) {
            redirectAttributes.addFlashAttribute("error", "Cette machine possède une adresse IP. Veuillez la reclasser en principale ou secours avant d'enregistrer.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/create?entrepriseId=" + machineForm.getEntrepriseId();
        }
        if (estMachineEntrepot && !StringUtils.hasText(machineForm.getNumeroSerie())) {
            redirectAttributes.addFlashAttribute("error", "Pour un matériel neuf, le numéro de série est obligatoire.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/create?entrepriseId=" + machineForm.getEntrepriseId();
        }
        if (!estMachineEntrepot && !StringUtils.hasText(machineForm.getNumeroSerie()) && !StringUtils.hasText(machineForm.getAdresseIP())) {
            redirectAttributes.addFlashAttribute("error", "Veuillez renseigner au moins le numéro de série ou l'adresse IP.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/create?entrepriseId=" + machineForm.getEntrepriseId();
        }
        
        // Plus besoin de valider le rattachement lors de la création
        // Une machine de secours peut être créée sans être rattachée immédiatement
        // Le rattachement se fera ensuite via le bouton "rattacher"

        try {
            Machine machine = machineForm.toMachine();
            machine.setPhotos(new ArrayList<>());
            populateCategoryDetails(machine, machineForm);
            
            if (estMachineEntrepot) {
                // Matériel neuf : non opérationnel et en programmation
                machine.setOperationnel(false);
                machine.setEnProgrammation(true);
                machine.setEnReparation(false);
                machine.setEstMachineSecours(false);
                machine.setMachinePrincipaleId(null);
            } else {
                // Machines programmées
                machine.setEnProgrammation(false);
                
                // Synchronisation automatique : non fonctionnel -> en réparation
                if (machine.getOperationnel() != null && !machine.getOperationnel()) {
                    machine.setEnReparation(true);
                }
                // Synchronisation automatique : en réparation -> non fonctionnel
                if (machine.getEnReparation() != null && machine.getEnReparation()) {
                    machine.setOperationnel(false);
                }
            }
            
            machine.setSupprime(false);

            String machineId = machineService.createMachine(machineForm.getEntrepriseId(), machine);
            
            // Préserver la session après création
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", machineForm.getEntrepriseId());

            List<String> uploadedPhotos = localFileStorageService.saveMachinePhotos(
                machineForm.getEntrepriseId(),
                machineId,
                photoFiles,
                3
            );

            if (!uploadedPhotos.isEmpty()) {
                machine.setPhotos(uploadedPhotos);
                machineService.updateMachine(machineForm.getEntrepriseId(), machineId, machine);
            }
            redirectAttributes.addFlashAttribute("success", "Machine créée avec succès.");
            // Préserver l'entrepriseId dans la session (session déjà déclarée plus haut)
            session.setAttribute("lastSelectedEntrepriseId", machineForm.getEntrepriseId());
            session.setAttribute("authenticated", true);
            // Toujours rediriger vers la liste avec l'entrepriseId pour garantir la cohérence
            return "redirect:/machines?entrepriseId=" + machineForm.getEntrepriseId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/create?entrepriseId=" + machineForm.getEntrepriseId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création de la machine: " + e.getMessage());
            return "redirect:/machines?entrepriseId=" + machineForm.getEntrepriseId();
        }
    }

    @GetMapping("/{entrepriseId}/{machineId}/edit")
    public String showEditForm(@PathVariable String entrepriseId,
                               @PathVariable String machineId,
                               Model model,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            // Nettoyer l'entrepriseId si plusieurs valeurs sont présentes
            if (StringUtils.hasText(entrepriseId) && entrepriseId.contains(",")) {
                entrepriseId = entrepriseId.split(",")[0].trim();
            }
            
            // Préserver la session
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            
            Machine machine = machineService.getMachine(entrepriseId, machineId);
            if (machine == null) {
                redirectAttributes.addFlashAttribute("error", "Machine introuvable.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }

            // Charger uniquement les machines principales (pas les machines de secours) pour permettre la sélection
            List<Machine> allMachines = machineService.listMachines(entrepriseId);
            List<Machine> machinesPrincipales = allMachines.stream()
                .filter(m -> m.getEstMachineSecours() == null || !m.getEstMachineSecours())
                .filter(m -> !m.getMachineId().equals(machineId)) // Exclure la machine en cours d'édition
                .collect(Collectors.toList());
            model.addAttribute("machines", machinesPrincipales);

            MachineForm form;
            Object existing = model.asMap().get("machineForm");
            if (existing instanceof MachineForm existingForm) {
                form = existingForm;
            } else {
                form = new MachineForm();
                form.setNom(machine.getNom());
                form.setNumeroSerie(machine.getNumeroSerie());
                form.setExistingPhotos(machine.getPhotos());
                form.setEmplacement(machine.getEmplacement());
                form.setNotes(machine.getNotes());
                form.setCategoryId(machine.getCategoryId());
                form.setOperationnel(machine.getOperationnel());
                form.setEnReparation(machine.getEnReparation());
                form.setEnProgrammation(machine.getEnProgrammation());
                form.setAdresseIP(machine.getAdresseIP());
                form.setMachinePrincipaleId(machine.getMachinePrincipaleId());
                form.setEstMachineSecours(machine.getEstMachineSecours());
                form.setEstMachineEntrepot(machine.getEstMachineEntrepot());
            }
            form.setEntrepriseId(entrepriseId);
            model.addAttribute("machineForm", form);
            model.addAttribute("machineId", machineId);
            model.addAttribute("entrepriseId", entrepriseId);
            model.addAttribute("existingPhotos", machine.getPhotos());
            model.addAttribute("machine", machine); // Pour afficher les infos de suivi
            model.addAttribute("categories", categoryService.findAll());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement de la machine: " + e.getMessage());
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        }

        return "machines/edit";
    }

    @PostMapping("/{entrepriseId}/{machineId}")
    public String updateMachine(@PathVariable String entrepriseId,
                                @PathVariable String machineId,
                                @ModelAttribute("machineForm") MachineForm machineForm,
                                @RequestParam(value = "machineType", required = false) String machineTypeSelection,
                                @RequestParam(value = "photoFiles", required = false) MultipartFile[] photoFiles,
                                @RequestParam(value = "deletePhotos", required = false) List<String> deletePhotos,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!StringUtils.hasText(machineForm.getNom())) {
            redirectAttributes.addFlashAttribute("error", "Le nom de la machine est obligatoire.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/edit";
        }
        
        applyMachineTypeSelection(machineForm, machineTypeSelection);

        // Validation : au moins numéro de série OU adresse IP doit être renseigné SAUF pour les machines entrepôt
        Boolean estMachineEntrepot = machineForm.getEstMachineEntrepot() != null ? machineForm.getEstMachineEntrepot() : false;
        if (estMachineEntrepot && StringUtils.hasText(machineForm.getAdresseIP())) {
            redirectAttributes.addFlashAttribute("error", "Cette machine possède une adresse IP. Choisissez un type principale ou secours avant d'enregistrer.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/edit";
        }
        if (estMachineEntrepot && !StringUtils.hasText(machineForm.getNumeroSerie())) {
            redirectAttributes.addFlashAttribute("error", "Pour un matériel neuf, le numéro de série est obligatoire.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/edit";
        }
        if (!estMachineEntrepot && !StringUtils.hasText(machineForm.getNumeroSerie()) && !StringUtils.hasText(machineForm.getAdresseIP())) {
            redirectAttributes.addFlashAttribute("error", "Veuillez renseigner au moins le numéro de série ou l'adresse IP.");
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/edit";
        }
        
        // Plus besoin de valider le rattachement lors de la modification
        // Le rattachement se fait via le bouton "rattacher"

        // Vérifier que l'utilisateur est superadmin pour modifier
        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut modifier des machines.");
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        }

        try {
            List<String> updatedPhotos = machineForm.getExistingPhotos() != null ? new ArrayList<>(machineForm.getExistingPhotos()) : new ArrayList<>();

            if (deletePhotos != null) {
                for (String photoUrl : deletePhotos) {
                    if (updatedPhotos.remove(photoUrl)) {
                        localFileStorageService.deletePhoto(photoUrl);
                    }
                }
            }

            int remainingSlots = Math.max(0, 3 - updatedPhotos.size());
            if (remainingSlots > 0) {
                List<String> uploadedPhotos = localFileStorageService.saveMachinePhotos(entrepriseId, machineId, photoFiles, remainingSlots);
                updatedPhotos.addAll(uploadedPhotos);
            }

            Machine machine = machineForm.toMachine();
            machine.setPhotos(updatedPhotos);
            populateCategoryDetails(machine, machineForm);

            boolean estActuellementEntrepot = machine.getEstMachineEntrepot() != null && machine.getEstMachineEntrepot();
            if (estActuellementEntrepot) {
                machine.setEnProgrammation(true);
                machine.setOperationnel(false);
                machine.setEnReparation(false);
                machine.setEstMachineSecours(false);
                machine.setMachinePrincipaleId(null);
            } else {
                machine.setEnProgrammation(false);
            }

            // Règles par défaut pour les machines programmées
            if (!estActuellementEntrepot) {
                if (machine.getOperationnel() != null && !machine.getOperationnel()) {
                    machine.setEnReparation(true);
                }
                if (machine.getEnReparation() != null && machine.getEnReparation()) {
                    machine.setOperationnel(false);
                }
            }

            // Récupérer la machine existante pour préserver certains champs
            Machine existingMachine = machineService.getMachine(entrepriseId, machineId);
            if (existingMachine != null) {
                // Gestion de la conversion d'une machine entrepôt en machine normale
                Boolean wasEntrepot = existingMachine.getEstMachineEntrepot() != null && existingMachine.getEstMachineEntrepot();
                if (wasEntrepot && !estMachineEntrepot) {
                    // La machine entrepôt devient une machine normale (principale ou secours)
                    machine.setEnProgrammation(false);
                    if (machine.getOperationnel() == null || !machine.getOperationnel()) {
                        machine.setOperationnel(true);
                    }
                    machine.setEnReparation(false);
                } else if (estMachineEntrepot) {
                    // Si c'est une machine entrepôt, masquer le type principale/secours
                    machine.setEstMachineSecours(false);
                    machine.setMachinePrincipaleId(null);
                }
                
                if (!estMachineEntrepot) {
                    // Si la machine était en réparation et qu'on la remet fonctionnelle, elle redevient une machine de secours
                    if (existingMachine.getEnReparation() != null && existingMachine.getEnReparation() 
                        && machine.getOperationnel() != null && machine.getOperationnel()
                        && machine.getEnReparation() != null && !machine.getEnReparation()) {
                        // La machine redevient une machine de secours (pas principale)
                        machine.setEstMachineSecours(true);
                        machine.setEnReparation(false);
                    } else if (machine.getEnReparation() != null && machine.getEnReparation()) {
                        // Si on met en réparation, la machine devient non fonctionnelle et n'est plus secours ni principale
                        machine.setOperationnel(false);
                        machine.setEstMachineSecours(false);
                        machine.setMachinePrincipaleId(null);
                    }
                    
                    // Si la machine devient non fonctionnelle, la mettre automatiquement en réparation
                    if (machine.getOperationnel() != null && !machine.getOperationnel() 
                        && (existingMachine.getOperationnel() == null || existingMachine.getOperationnel())) {
                        // La machine vient de devenir non fonctionnelle
                        machine.setEnReparation(true);
                        
                        // Vérifier si c'est une machine principale avec des machines de secours attachées
                        boolean isMachinePrincipale = existingMachine.getEstMachineSecours() == null || !existingMachine.getEstMachineSecours();
                        if (isMachinePrincipale) {
                            // Récupérer toutes les machines de secours attachées à cette machine principale
                            List<Machine> allMachines = machineService.listMachines(entrepriseId);
                            List<Machine> machinesSecoursRattachees = allMachines.stream()
                                .filter(m -> m.getEstMachineSecours() != null && m.getEstMachineSecours())
                                .filter(m -> m.getMachinePrincipaleId() != null && m.getMachinePrincipaleId().equals(machineId))
                                .filter(m -> m.getEnReparation() == null || !m.getEnReparation()) // Exclure celles déjà en réparation
                                .filter(m -> m.getOperationnel() != null && m.getOperationnel()) // Seulement les fonctionnelles
                                .collect(Collectors.toList());
                            
                            // Si des machines de secours sont disponibles, proposer de les promouvoir
                            if (!machinesSecoursRattachees.isEmpty()) {
                                // Sauvegarder d'abord l'état non fonctionnel et en réparation
                                machineService.updateMachine(entrepriseId, machineId, machine);
                                
                                // Rediriger vers la page de sélection de la machine de secours à promouvoir
                                redirectAttributes.addFlashAttribute("info", "Cette machine principale est maintenant non fonctionnelle. Veuillez sélectionner une machine de secours pour la remplacer.");
                                HttpSession session = request.getSession(true);
                                session.setAttribute("authenticated", true);
                                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
                                return "redirect:/machines/" + entrepriseId + "/" + machineId + "/promote-secours-form";
                            }
                        }
                    }
                }
            }

            machineService.updateMachine(entrepriseId, machineId, machine);
            redirectAttributes.addFlashAttribute("success", "Machine mise à jour avec succès.");
            // Préserver l'entrepriseId dans la session et maintenir l'authentification
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("machineForm", machineForm);
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour de la machine: " + e.getMessage());
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/edit";
        }
    }

    @PostMapping("/{entrepriseId}/{machineId}/delete")
    public String deleteMachine(@PathVariable String entrepriseId,
                                @PathVariable String machineId,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        // Vérifier que l'utilisateur est superadmin
        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut supprimer des machines.");
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        }

        try {
            Machine existing = machineService.getMachine(entrepriseId, machineId);
            if (existing != null) {
                // Supprimer les photos si nécessaire
                if (existing.getPhotos() != null) {
                for (String photo : existing.getPhotos()) {
                    localFileStorageService.deletePhoto(photo);
                    }
                }
            }

            // Supprimer vraiment de Firebase
            machineService.deleteMachine(entrepriseId, machineId);
            
            redirectAttributes.addFlashAttribute("success", "Machine supprimée avec succès.");
            // Préserver l'entrepriseId dans la session et maintenir l'authentification
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticated", true);
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression de la machine: " + e.getMessage());
        }

        return "redirect:/machines?entrepriseId=" + entrepriseId;
    }

    private void populateCategoryDetails(Machine machine, MachineForm machineForm) {
        if (machineForm.getCategoryId() == null) {
            machine.setCategoryId(null);
            machine.setCategoryName(null);
            machine.setCategoryDescription(null);
            return;
        }

        Category category = categoryService.getCategory(machineForm.getCategoryId());
        machine.setCategoryId(category.getId());
        machine.setCategoryName(category.getName());
        machine.setCategoryDescription(category.getDescription());
    }

    private void applyMachineTypeSelection(MachineForm machineForm, String machineTypeSelection) {
        if (machineForm == null || !StringUtils.hasText(machineTypeSelection)) {
            return;
        }

        String normalized = machineTypeSelection.trim().toLowerCase();
        switch (normalized) {
            case "secours" -> {
                machineForm.setEstMachineSecours(true);
                machineForm.setEstMachineEntrepot(false);
            }
            case "entrepot" -> {
                machineForm.setEstMachineSecours(false);
                machineForm.setEstMachineEntrepot(true);
                machineForm.setMachinePrincipaleId(null);
            }
            default -> {
                machineForm.setEstMachineSecours(false);
                machineForm.setEstMachineEntrepot(false);
                machineForm.setMachinePrincipaleId(null);
            }
        }
    }
}

