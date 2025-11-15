package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.entity.Category;
import com.maintenance.maintenance.model.entity.SubCategory;
import com.maintenance.maintenance.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

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
    public String listCategories(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        model.addAttribute("isSuperAdmin", isSuperAdmin(request));
        return "categories/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!model.containsAttribute("category")) {
            model.addAttribute("category", new Category());
        }
        return "categories/create";
    }

    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("category") Category category,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 HttpServletRequest request) {
        System.out.println("=== CategoryController.createCategory: DÉBUT ===");
        System.out.println("Category reçue - Nom: " + (category != null ? category.getName() : "null") + ", ID: " + (category != null ? category.getId() : "null"));
        
        if (!ensureAuthenticated(request, redirectAttributes)) {
            System.out.println("=== CategoryController.createCategory: Non authentifié, redirection ===");
            return "redirect:/login";
        }

        // Préserver la session
        HttpSession session = request.getSession(true);
        session.setAttribute("authenticated", true);

        if (bindingResult.hasErrors()) {
            System.out.println("=== CategoryController.createCategory: Erreurs de validation ===");
            redirectAttributes.addFlashAttribute("error", "Veuillez corriger les erreurs du formulaire.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.category", bindingResult);
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/categories/create";
        }

        try {
            System.out.println("=== CategoryController.createCategory: Appel à categoryService.createCategory ===");
            Category created = categoryService.createCategory(category);
            System.out.println("=== CategoryController.createCategory: Catégorie créée avec succès - ID: " + (created != null ? created.getId() : "null") + " ===");
            redirectAttributes.addFlashAttribute("success", "Catégorie créée avec succès.");
            return "redirect:/categories";
        } catch (IllegalArgumentException e) {
            System.err.println("=== CategoryController.createCategory: IllegalArgumentException - " + e.getMessage() + " ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/categories/create";
        } catch (Exception e) {
            System.err.println("=== CategoryController.createCategory: Exception - " + e.getMessage() + " ===");
            System.err.println("=== CategoryController.createCategory: Stack trace ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/categories/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        // Vérifier que l'utilisateur est superadmin
        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut modifier des catégories.");
            return "redirect:/categories";
        }

        try {
            Category category = categoryService.getCategory(id);
            if (!model.containsAttribute("category")) {
                model.addAttribute("category", category);
            }
            return "categories/edit";
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("category") Category category,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        // Vérifier que l'utilisateur est superadmin
        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut modifier des catégories.");
            return "redirect:/categories";
        }

        // Préserver la session
        HttpSession session = request.getSession(true);
        session.setAttribute("authenticated", true);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez corriger les erreurs du formulaire.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.category", bindingResult);
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/categories/" + id + "/edit";
        }

        try {
            categoryService.updateCategory(id, category);
            redirectAttributes.addFlashAttribute("success", "Catégorie mise à jour avec succès.");
            return "redirect:/categories";
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/categories/" + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/categories/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes,
                                 HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        // Vérifier que l'utilisateur est superadmin
        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Seul le super administrateur peut supprimer des catégories.");
            return "redirect:/categories";
        }

        // Préserver la session
        HttpSession session = request.getSession(true);
        session.setAttribute("authenticated", true);

        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Catégorie supprimée avec succès.");
            return "redirect:/categories";
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
            return "redirect:/categories";
        }
    }

    @GetMapping("/{categoryId}/subcategories/create")
    public String showCreateSubCategoryForm(@PathVariable Long categoryId,
                                            Model model,
                                            HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Category category = categoryService.getCategory(categoryId);
            model.addAttribute("category", category);
            if (!model.containsAttribute("subCategory")) {
                model.addAttribute("subCategory", new SubCategory());
            }
            return "categories/subcategories/create";
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    @PostMapping("/{categoryId}/subcategories/create")
    public String createSubCategory(@PathVariable Long categoryId,
                                    @Valid @ModelAttribute("subCategory") SubCategory subCategory,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez corriger les erreurs du formulaire.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.subCategory", bindingResult);
            redirectAttributes.addFlashAttribute("subCategory", subCategory);
            return "redirect:/categories/" + categoryId + "/subcategories/create";
        }

        try {
            categoryService.createSubCategory(categoryId, subCategory);
            redirectAttributes.addFlashAttribute("success", "Sous-catégorie créée avec succès.");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("subCategory", subCategory);
            return "redirect:/categories/" + categoryId + "/subcategories/create";
        }

        return "redirect:/categories";
    }

    @GetMapping("/{categoryId}/subcategories/{subCategoryId}/edit")
    public String showEditSubCategoryForm(@PathVariable Long categoryId,
                                          @PathVariable Long subCategoryId,
                                          Model model,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Category category = categoryService.getCategory(categoryId);
            SubCategory subCategory = categoryService.getSubCategory(subCategoryId);

            if (!subCategory.getCategory().getId().equals(categoryId)) {
                redirectAttributes.addFlashAttribute("error", "La sous-catégorie ne correspond pas à la catégorie sélectionnée.");
                return "redirect:/categories";
            }

            model.addAttribute("category", category);
            if (!model.containsAttribute("subCategory")) {
                model.addAttribute("subCategory", subCategory);
            }
            return "categories/subcategories/edit";
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categories";
        }
    }

    @PostMapping("/{categoryId}/subcategories/{subCategoryId}/edit")
    public String updateSubCategory(@PathVariable Long categoryId,
                                    @PathVariable Long subCategoryId,
                                    @Valid @ModelAttribute("subCategory") SubCategory subCategory,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez corriger les erreurs du formulaire.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.subCategory", bindingResult);
            redirectAttributes.addFlashAttribute("subCategory", subCategory);
            return "redirect:/categories/" + categoryId + "/subcategories/" + subCategoryId + "/edit";
        }

        try {
            categoryService.updateSubCategory(categoryId, subCategoryId, subCategory);
            redirectAttributes.addFlashAttribute("success", "Sous-catégorie mise à jour avec succès.");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("subCategory", subCategory);
            return "redirect:/categories/" + categoryId + "/subcategories/" + subCategoryId + "/edit";
        }

        return "redirect:/categories";
    }

    @PostMapping("/{categoryId}/subcategories/{subCategoryId}/delete")
    public String deleteSubCategory(@PathVariable Long categoryId,
                                    @PathVariable Long subCategoryId,
                                    RedirectAttributes redirectAttributes,
                                    HttpServletRequest request) {
        if (!ensureAuthenticated(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            categoryService.deleteSubCategory(categoryId, subCategoryId);
            redirectAttributes.addFlashAttribute("success", "Sous-catégorie supprimée avec succès.");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/categories";
    }
}


