package com.maintenance.maintenance.repository;

import com.maintenance.maintenance.model.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {
    
    Optional<Enterprise> findByFirebaseId(String firebaseId);
    
    Optional<Enterprise> findByNom(String nom);
}

