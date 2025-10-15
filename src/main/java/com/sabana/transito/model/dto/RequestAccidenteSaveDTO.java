package com.sabana.transito.model.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestAccidenteSaveDTO {
    private String direccion;
    private String tipo;
    private String fecha;
}
