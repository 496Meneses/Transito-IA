package com.sabana.transito.controller;
import com.sabana.transito.model.records.AccidentRecord;
import com.sabana.transito.model.dto.MensajeDTO;
import com.sabana.transito.model.dto.RequestAccidenteSaveDTO;
import com.sabana.transito.services.AccidentEmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/accidentes")
public class AccidentController {

    private final AccidentEmbeddingService service;
    public AccidentController(AccidentEmbeddingService service) {
        this.service = service;
    }

    @Operation(
            summary = "Buscar accidentes similares",
            description = "Devuelve una lista de registros de accidentes con direcciones similares a la proporcionada."
    )
    @GetMapping("/similares")
    public List<AccidentRecord> buscarSimilares(
            @Parameter(description = "Dirección de referencia para buscar accidentes similares", required = true)
            @RequestParam String direccion) {
        return service.buscarSimilares(direccion);
    }

    @Operation(
            summary = "Generar alerta de accidentes",
            description = "Genera una alerta basada en la dirección proporcionada, indicando posibles riesgos o coincidencias de accidentes."
    )
    @GetMapping("/alerta")
    public String generarAlerta(
            @Parameter(description = "Dirección de referencia para generar la alerta", required = true)
            @RequestParam String direccion) {
        return service.generarAlerta(direccion);
    }
    @Operation(
            summary = "Generar alerta de accidentes",
            description = "Genera una alerta basada en la dirección proporcionada, indicando posibles riesgos o coincidencias de accidentes. fecha en formato DD/MM/YYYY"
    )
    @PostMapping("/guardar")
    private MensajeDTO guardarAccidente(@RequestBody RequestAccidenteSaveDTO dto) {
        return service.guardarAccidente(dto);
    }
}
