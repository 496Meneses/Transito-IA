package com.sabana.transito.controller;
import com.sabana.transito.model.AccidentRecord;
import com.sabana.transito.services.AccidentEmbeddingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/accidentes")
public class AccidentController {

    private final AccidentEmbeddingService service;
    public AccidentController(AccidentEmbeddingService service) {
        this.service = service;
    }

    @GetMapping("/similares")
    public List<AccidentRecord> buscarSimilares(@RequestParam String direccion) {
        return service.buscarSimilares(direccion);
    }

    @GetMapping("/alerta")
    public String generarAlerta(@RequestParam String direccion) {
        return service.generarAlerta(direccion);
    }
}
