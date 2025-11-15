package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.dto.StockParCategorie;
import com.maintenance.maintenance.model.entity.Category;
import com.maintenance.maintenance.model.entity.Machine;
import com.maintenance.maintenance.service.CategoryService;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import com.maintenance.maintenance.service.MachineService;
import com.maintenance.maintenance.service.StockService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private MachineService machineService;

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

    @GetMapping
    public String listStocks(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
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

            List<StockParCategorie> stocksParCategorie = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                stocksParCategorie = stockService.calculerStockParCategorie(entrepriseId);
                session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            }

            model.addAttribute("enterprises", enterprises);
            model.addAttribute("selectedEntrepriseId", entrepriseId != null ? entrepriseId : "");
            model.addAttribute("stocksParCategorie", stocksParCategorie);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement du stock: " + e.getMessage());
            return "redirect:/dashboard";
        }

        return "stock/list";
    }

    @GetMapping("/{entrepriseId}/categorie/{categoryId}")
    public String voirDetailsCategorie(@PathVariable String entrepriseId,
                                      @PathVariable Long categoryId,
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
            
            // Nettoyer entrepriseId si plusieurs valeurs
            if (entrepriseId != null && entrepriseId.contains(",")) {
                entrepriseId = entrepriseId.split(",")[0].trim();
            }
            
            // Vérifier que les paramètres sont valides
            if (!StringUtils.hasText(entrepriseId)) {
                redirectAttributes.addFlashAttribute("error", "Identifiant d'entreprise invalide.");
                return "redirect:/stock";
            }
            
            if (categoryId == null) {
                redirectAttributes.addFlashAttribute("error", "Identifiant de catégorie invalide.");
                return "redirect:/stock?entrepriseId=" + entrepriseId;
            }
            
            session.setAttribute("lastSelectedEntrepriseId", entrepriseId);
            
            List<Machine> machines = stockService.getMachinesParCategorie(entrepriseId, categoryId);
            if (machines == null) {
                machines = new ArrayList<>();
            }
            
            // Filtrer les machines null et s'assurer que la liste est valide
            List<Machine> validMachines = new ArrayList<>();
            for (Machine m : machines) {
                if (m != null) {
                    validMachines.add(m);
                }
            }
            machines = validMachines;
            
            // Charger toutes les machines de l'entreprise pour créer un map complet
            // (nécessaire pour afficher le nom des machines principales même si elles sont dans d'autres catégories)
            List<Machine> allMachines = machineService.listMachines(entrepriseId);
            Map<String, Machine> machinesMap = new HashMap<>();
            if (allMachines != null) {
                for (Machine m : allMachines) {
                    if (m != null && m.getMachineId() != null) {
                        machinesMap.put(m.getMachineId(), m);
                    }
                }
            }
            
            model.addAttribute("machines", machines);
            model.addAttribute("machinesMap", machinesMap != null ? machinesMap : new HashMap<>()); // Pour l'affichage dans la vue
            model.addAttribute("entrepriseId", entrepriseId);
            model.addAttribute("selectedEntrepriseId", entrepriseId);
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("machinesCount", machines.size()); // Pour debug
            
            // Récupérer le nom de la catégorie
            Category category = null;
            try {
                category = categoryService.getCategory(categoryId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (category != null) {
                model.addAttribute("categoryName", category.getName() != null ? category.getName() : "Catégorie");
                model.addAttribute("categoryDescription", category.getDescription() != null ? category.getDescription() : "");
            } else {
                model.addAttribute("categoryName", "Catégorie");
                model.addAttribute("categoryDescription", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des machines: " + e.getMessage());
            return "redirect:/stock?entrepriseId=" + (entrepriseId != null ? entrepriseId : "");
        }

        return "stock/details-categorie";
    }
}

