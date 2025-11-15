package com.maintenance.maintenance.repository;

import com.maintenance.maintenance.model.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findByCategoryIdOrderByNameAsc(Long categoryId);

    boolean existsByNameIgnoreCaseAndCategoryId(String name, Long categoryId);
}


