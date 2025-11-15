package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Category;
import com.maintenance.maintenance.model.entity.SubCategory;
import com.maintenance.maintenance.repository.CategoryRepository;
import com.maintenance.maintenance.repository.SubCategoryRepository;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        try {
            // Récupérer depuis Firebase
            List<Category> categories = firebaseRealtimeService.getAllCategories();
            // Trier par nom
            return categories.stream()
                    .sorted((a, b) -> {
                        if (a.getName() == null) return 1;
                        if (b.getName() == null) return -1;
                        return a.getName().compareToIgnoreCase(b.getName());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Fallback sur JPA si Firebase échoue
            System.err.println("Erreur Firebase, utilisation de JPA: " + e.getMessage());
            return categoryRepository.findAllByOrderByNameAsc();
        }
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        try {
            // Essayer Firebase d'abord
            List<Category> categories = firebaseRealtimeService.getAllCategories();
            Category category = categories.stream()
                    .filter(c -> c.getId() != null && c.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            
            if (category != null) {
                return category;
            }
            
            // Fallback sur JPA
            return categoryRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id));
        } catch (Exception e) {
            // Si Firebase échoue, essayer JPA
            try {
                return categoryRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id));
            } catch (Exception jpaException) {
                throw new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id);
            }
        }
    }

    @Transactional(readOnly = true)
    public SubCategory getSubCategory(Long id) {
        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sous-catégorie introuvable pour l'identifiant " + id));
    }

    @Transactional
    public Category createCategory(Category category) {
        System.out.println("=== CategoryService.createCategory: DÉBUT ===");
        System.out.println("Category reçue - Nom: " + (category != null ? category.getName() : "null") + ", ID: " + (category != null ? category.getId() : "null"));
        
        normalizeCategory(category);
        System.out.println("=== CategoryService.createCategory: Après normalizeCategory ===");
        
        // Vérifier que le nom n'est pas vide
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            System.err.println("=== CategoryService.createCategory: Nom vide ===");
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }
        
        // S'assurer que l'ID est null pour éviter les conflits avec JPA
        // Firebase générera son propre ID
        Category categoryToCreate = new Category();
        categoryToCreate.setName(category.getName());
        categoryToCreate.setDescription(category.getDescription());
        categoryToCreate.setId(null); // S'assurer que l'ID est null
        System.out.println("=== CategoryService.createCategory: categoryToCreate créé - Nom: " + categoryToCreate.getName() + ", ID: " + categoryToCreate.getId() + " ===");
        
        try {
            // Vérifier si une catégorie avec ce nom existe déjà dans Firebase
            System.out.println("=== CategoryService.createCategory: Vérification des catégories existantes ===");
            List<Category> existingCategories = new ArrayList<>();
            try {
                existingCategories = firebaseRealtimeService.getAllCategories();
                System.out.println("=== CategoryService.createCategory: Nombre de catégories existantes: " + (existingCategories != null ? existingCategories.size() : 0) + " ===");
                if (existingCategories == null) {
                    existingCategories = new ArrayList<>();
                }
            } catch (Exception e) {
                // Si le nœud "categories" n'existe pas encore dans Firebase, c'est normal pour la première création
                System.out.println("=== CategoryService.createCategory: Aucune catégorie existante dans Firebase (première création): " + e.getMessage() + " ===");
                existingCategories = new ArrayList<>();
            }
            
            boolean exists = existingCategories.stream()
                    .anyMatch(c -> c != null && c.getName() != null && c.getName().equalsIgnoreCase(categoryToCreate.getName()));
            if (exists) {
                System.err.println("=== CategoryService.createCategory: Catégorie existe déjà ===");
                throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà");
            }
            
            // Créer dans Firebase
            System.out.println("=== CategoryService.createCategory: Appel à firebaseRealtimeService.createCategory ===");
            System.out.println("CategoryToCreate - Nom: " + categoryToCreate.getName() + ", Description: " + categoryToCreate.getDescription() + ", ID: " + categoryToCreate.getId());
            String categoryId = firebaseRealtimeService.createCategory(categoryToCreate);
            System.out.println("=== CategoryService.createCategory: Catégorie créée dans Firebase avec ID Firebase: " + categoryId + " ===");
            
            // Récupérer la catégorie créée
            System.out.println("=== CategoryService.createCategory: Appel à firebaseRealtimeService.getCategoryById avec ID: " + categoryId + " ===");
            Category created = firebaseRealtimeService.getCategoryById(categoryId);
            System.out.println("=== CategoryService.createCategory: Catégorie récupérée depuis Firebase - Nom: " + (created != null ? created.getName() : "null") + ", ID: " + (created != null ? created.getId() : "null") + " ===");
            
            // Ne pas sauvegarder dans JPA pour éviter les conflits d'ID
            // Firebase est maintenant la source principale de vérité pour les catégories
            // JPA est utilisé uniquement comme fallback si Firebase échoue
            
            System.out.println("=== CategoryService.createCategory: SUCCÈS - Retour de la catégorie créée ===");
            return created != null ? created : categoryToCreate;
        } catch (IllegalArgumentException e) {
            System.err.println("=== CategoryService.createCategory: IllegalArgumentException - " + e.getMessage() + " ===");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            // Fallback sur JPA si Firebase échoue
            System.err.println("=== CategoryService.createCategory: Exception Firebase - " + e.getMessage() + " ===");
            System.err.println("=== CategoryService.createCategory: Stack trace ===");
            e.printStackTrace();
            System.out.println("=== CategoryService.createCategory: Fallback sur JPA ===");
            if (categoryRepository.existsByNameIgnoreCase(categoryToCreate.getName())) {
                throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà");
            }
            // S'assurer que l'ID est null pour que JPA le génère automatiquement
            categoryToCreate.setId(null);
            Category saved = categoryRepository.save(categoryToCreate);
            System.out.println("=== CategoryService.createCategory: Catégorie sauvegardée dans JPA avec ID: " + saved.getId() + " ===");
            return saved;
        }
    }

    @Transactional
    public Category updateCategory(Long id, Category updated) {
        normalizeCategory(updated);
        
        try {
            // Récupérer depuis Firebase (convertir l'ID)
            // Pour l'instant, on utilise JPA comme source principale
            Category existing = categoryRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id));

            // Vérifier les doublons dans Firebase
            List<Category> existingCategories = firebaseRealtimeService.getAllCategories();
            boolean duplicateExists = existingCategories.stream()
                    .anyMatch(c -> c.getId() != null && !c.getId().equals(id) 
                            && c.getName() != null && c.getName().equalsIgnoreCase(updated.getName()));
            if (duplicateExists) {
                throw new IllegalArgumentException("Une autre catégorie porte déjà ce nom");
            }

            existing.setName(updated.getName());
            existing.setDescription(updated.getDescription());
            
            // Mettre à jour dans Firebase si possible
            try {
                // Chercher la catégorie dans Firebase par nom (car on n'a pas l'ID Firebase directement)
                List<Category> firebaseCategories = firebaseRealtimeService.getAllCategories();
                Category firebaseCategory = firebaseCategories.stream()
                        .filter(c -> c.getId() != null && c.getId().equals(id))
                        .findFirst()
                        .orElse(null);
                
                if (firebaseCategory != null) {
                    // Trouver l'ID Firebase correspondant
                    // Pour cela, on doit chercher dans Firebase avec le nom ou créer un mapping
                    // Pour l'instant, on essaie de mettre à jour via JPA d'abord, puis Firebase
                    // TODO: Améliorer le mapping ID Long <-> ID Firebase String
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour Firebase (non bloquant): " + e.getMessage());
            }
            
            return categoryRepository.save(existing);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            // Fallback sur JPA uniquement
            Category existing = categoryRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id));

            normalizeCategory(updated);

            if (!existing.getName().equalsIgnoreCase(updated.getName())
                    && categoryRepository.existsByNameIgnoreCase(updated.getName())) {
                throw new IllegalArgumentException("Une autre catégorie porte déjà ce nom");
            }

            existing.setName(updated.getName());
            existing.setDescription(updated.getDescription());
            return categoryRepository.save(existing);
        }
    }

    @Transactional
    public void deleteCategory(Long id) {
        try {
            // Supprimer de Firebase
            try {
                // Chercher la catégorie dans Firebase par ID Long
                List<Category> firebaseCategories = firebaseRealtimeService.getAllCategories();
                Category firebaseCategory = firebaseCategories.stream()
                        .filter(c -> c.getId() != null && c.getId().equals(id))
                        .findFirst()
                        .orElse(null);
                
                // Pour l'instant, on supprime via JPA d'abord
                // TODO: Améliorer le mapping ID Long <-> ID Firebase String pour supprimer directement
            } catch (Exception e) {
                System.err.println("Erreur lors de la suppression Firebase (non bloquant): " + e.getMessage());
            }
            
            // Supprimer de JPA
            if (!categoryRepository.existsById(id)) {
                throw new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id);
            }
            categoryRepository.deleteById(id);
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            // Fallback sur JPA uniquement
            if (!categoryRepository.existsById(id)) {
                throw new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id);
            }
            categoryRepository.deleteById(id);
        }
    }

    @Transactional
    public SubCategory createSubCategory(Long categoryId, SubCategory subCategory) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + categoryId));

        normalizeSubCategory(subCategory);

        if (subCategoryRepository.existsByNameIgnoreCaseAndCategoryId(subCategory.getName(), categoryId)) {
            throw new IllegalArgumentException("Une sous-catégorie avec ce nom existe déjà dans cette catégorie");
        }

        subCategory.setCategory(category);
        return subCategoryRepository.save(subCategory);
    }

    @Transactional
    public SubCategory updateSubCategory(Long categoryId, Long subCategoryId, SubCategory updated) {
        SubCategory existing = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Sous-catégorie introuvable pour l'identifiant " + subCategoryId));

        if (!existing.getCategory().getId().equals(categoryId)) {
            throw new IllegalArgumentException("La sous-catégorie ne correspond pas à la catégorie fournie");
        }

        normalizeSubCategory(updated);

        boolean duplicate = subCategoryRepository.existsByNameIgnoreCaseAndCategoryId(updated.getName(), categoryId)
                && !existing.getName().equalsIgnoreCase(updated.getName());
        if (duplicate) {
            throw new IllegalArgumentException("Une autre sous-catégorie porte déjà ce nom dans cette catégorie");
        }

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        return subCategoryRepository.save(existing);
    }

    @Transactional
    public void deleteSubCategory(Long categoryId, Long subCategoryId) {
        SubCategory existing = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Sous-catégorie introuvable pour l'identifiant " + subCategoryId));

        if (!existing.getCategory().getId().equals(categoryId)) {
            throw new IllegalArgumentException("La sous-catégorie ne correspond pas à la catégorie fournie");
        }

        subCategoryRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<SubCategory> listSubCategories(Long categoryId) {
        return subCategoryRepository.findByCategoryIdOrderByNameAsc(categoryId);
    }

    private void normalizeCategory(Category category) {
        category.setName(category.getName() != null ? category.getName().trim() : null);
        category.setDescription(category.getDescription() != null ? category.getDescription().trim() : null);
    }

    private void normalizeSubCategory(SubCategory subCategory) {
        subCategory.setName(subCategory.getName() != null ? subCategory.getName().trim() : null);
        subCategory.setDescription(subCategory.getDescription() != null ? subCategory.getDescription().trim() : null);
    }
}


