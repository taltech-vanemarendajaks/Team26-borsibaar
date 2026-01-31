package com.borsibaar.repository;

import com.borsibaar.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);

    List<Inventory> findByProduct_OrganizationId(Long organizationId);

    List<Inventory> findByProduct_OrganizationIdAndProduct_CategoryId(Long organizationId, Long categoryId);

    boolean existsByProductId(Long productId);
}
