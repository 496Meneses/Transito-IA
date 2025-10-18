package com.sabana.transito.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.repository.NoRepositoryBean;

@Getter
@Setter
@AllArgsConstructor
@NoRepositoryBean
public class ReponseGenerarAlerta {
    String mensaje;
}
