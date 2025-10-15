package com.sabana.transito.model.repositories;

import com.sabana.transito.model.entidades.Accidente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccidenteRepository extends JpaRepository<Accidente, Long> {
}
