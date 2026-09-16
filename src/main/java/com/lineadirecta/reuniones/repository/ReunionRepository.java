package com.lineadirecta.reuniones.repository;

import com.lineadirecta.reuniones.domain.Reunion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReunionRepository extends JpaRepository<Reunion, Long>, JpaSpecificationExecutor<Reunion> {
}
