package com.maintenance.maintenance.repository;

import com.maintenance.maintenance.model.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = "subCategories")
    List<Category> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "subCategories")
    Optional<Category> findWithSubCategoriesById(Long id);

    boolean existsByNameIgnoreCase(String name);
}


