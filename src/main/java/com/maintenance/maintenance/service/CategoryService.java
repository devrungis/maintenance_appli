package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Category;
import com.maintenance.maintenance.model.entity.SubCategory;
import com.maintenance.maintenance.repository.CategoryRepository;
import com.maintenance.maintenance.repository.SubCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    public List<Category> findAll() {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        if (!categories.isEmpty()) {
            return categories;
        }
        importCategoriesFromFirebase();
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    protected void importCategoriesFromFirebase() {
        try {
            List<Category> firebaseCategories = firebaseRealtimeService.getAllCategories();
            if (firebaseCategories == null || firebaseCategories.isEmpty()) {
                return;
            }
            for (Category firebaseCategory : firebaseCategories) {
                if (firebaseCategory == null || firebaseCategory.getName() == null) {
                    continue;
                }
                if (categoryRepository.existsByNameIgnoreCase(firebaseCategory.getName())) {
                    continue;
                }
                Category entity = new Category();
                entity.setName(firebaseCategory.getName());
                entity.setDescription(firebaseCategory.getDescription());
                categoryRepository.save(entity);
            }
        } catch (Exception e) {
            System.err.println("Impossible de récupérer les catégories Firebase: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id));
    }

    @Transactional(readOnly = true)
    public SubCategory getSubCategory(Long id) {
        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sous-catégorie introuvable pour l'identifiant " + id));
    }

    @Transactional
    public Category createCategory(Category category) {
        normalizeCategory(category);
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }
        if (categoryRepository.existsByNameIgnoreCase(category.getName())) {
            throw new IllegalArgumentException("Une catégorie avec ce nom existe déjà");
        }
        category.setId(null);
        Category saved = categoryRepository.save(category);
        syncCategoryToFirebase(saved);
        return saved;
    }

    @Transactional
    public Category updateCategory(Long id, Category updated) {
        normalizeCategory(updated);
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id));

        if (!existing.getName().equalsIgnoreCase(updated.getName())
                && categoryRepository.existsByNameIgnoreCase(updated.getName())) {
            throw new IllegalArgumentException("Une autre catégorie porte déjà ce nom");
        }

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        Category saved = categoryRepository.save(existing);
        syncCategoryToFirebase(saved);
        return saved;
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Catégorie introuvable pour l'identifiant " + id);
        }
        categoryRepository.deleteById(id);
        removeCategoryFromFirebase(id);
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

    private void syncCategoryToFirebase(Category category) {
        try {
            firebaseRealtimeService.updateCategory(String.valueOf(category.getId()), category);
        } catch (Exception e) {
            System.err.println("Impossible de synchroniser la catégorie dans Firebase: " + e.getMessage());
        }
    }

    private void removeCategoryFromFirebase(Long id) {
        try {
            firebaseRealtimeService.deleteCategory(String.valueOf(id));
        } catch (Exception e) {
            System.err.println("Impossible de supprimer la catégorie de Firebase (non bloquant): " + e.getMessage());
        }
    }
}


