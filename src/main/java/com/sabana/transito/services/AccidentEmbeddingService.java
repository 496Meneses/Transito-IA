package com.sabana.transito.services;

import com.sabana.transito.model.records.AccidentRecord;
import com.sabana.transito.model.entidades.Accidente;
import com.sabana.transito.model.dto.MensajeDTO;
import com.sabana.transito.model.dto.RequestAccidenteSaveDTO;
import com.sabana.transito.model.repositories.AccidenteRepository;
import com.sabana.transito.utilidades.FechaUtil;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccidentEmbeddingService {

    private final List<AccidentRecord> registros = new ArrayList<>();

    private final OpenAiEmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final AccidenteRepository accidenteRepository;

    private static final int TOP_K = 10;
    private static final int LIMITE_DATASET = 100;

    public AccidentEmbeddingService(OpenAiEmbeddingModel modeloEmbeddings, ChatModel chatModel, AccidenteRepository accidenteRepository) throws Exception {
        this.embeddingModel = modeloEmbeddings;
        this.chatModel = chatModel;
        this.accidenteRepository = accidenteRepository;
        cargarYProcesarDataset();
    }

    public MensajeDTO guardarAccidente(RequestAccidenteSaveDTO accidente) {
        try {
            Accidente accidenteNuevo = accidenteRepository.save(Accidente.builder().direccion(accidente.getDireccion())
                    .tipo(accidente.getTipo())
                    .fecha(FechaUtil.convertirStringALocalDate(accidente.getFecha()))
                    .build());
            this.generarYGuardarEmbeddings(List.of(accidenteNuevo));
            return new MensajeDTO("Accidente guardado exitosamente", false);
        } catch (Exception e) {
            return new MensajeDTO("Error al guardar el accidente: " + e.getMessage(), true);
        }
    }

    private void cargarYProcesarDataset() throws Exception {
        var recurso = new ClassPathResource("dataset.csv");
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            procesarArchivo(lector);
        }
    }
    private void procesarArchivo(BufferedReader lector) {
        List<String> lineas = lector.lines()
                .skip(1)
                .limit(LIMITE_DATASET)
                .toList();

        List<String[]> filasValidas = obtenerFilasValidas(lineas);
        List<String> descripciones = generarDescripciones(filasValidas);
        generarYGuardarEmbeddings(filasValidas, descripciones);
    }
    private List<String[]> obtenerFilasValidas(List<String> lineas) {
        List<String[]> filas = new ArrayList<>();
        for (String linea : lineas) {
            String[] columnas = linea.split(",");
            if (columnas.length >= 11) {
                filas.add(columnas);
            }
        }
        return filas;
    }
    private List<String> generarDescripciones(List<String[]> filas) {
        List<String> descripciones = new ArrayList<>();
        for (String[] columnas : filas) {
            descripciones.add(construirDescripcion(columnas));
        }
        return descripciones;
    }

    private String construirDescripcion(String[] columnas) {
        String direccion = columnas[7].trim();
        String gravedad = columnas[8].trim();
        String clase = columnas[9].trim();
        String localidad = columnas[10].trim();

        return String.format("%s con %s en %s, %s", clase, gravedad, localidad, direccion);
    }

    private String construirDescripcionParaEntidadAccidente(Accidente accidente) {
        String direccion = accidente.getDireccion() != null ? accidente.getDireccion().trim() : "";
        String gravedad = "desconocida";
        String clase = accidente.getTipo() != null ? accidente.getTipo().trim() : "desconocido";
        String localidad = "desconocida";
        return String.format("%s con %s en %s, %s", clase, gravedad, localidad, direccion);
    }

    private void generarYGuardarEmbeddings(List<Accidente> accidentes) {
        List<String> descripciones = accidentes.stream()
                .map(this::construirDescripcionParaEntidadAccidente)
                .collect(Collectors.toList());

        EmbeddingResponse respuesta = embeddingModel.call(new EmbeddingRequest(descripciones, null));
        var resultados = respuesta.getResults();

        for (int i = 0; i < accidentes.size(); i++) {
            Accidente accidente = accidentes.get(i);
            float[] embedding = resultados.get(i).getOutput();

            if (registros.stream().anyMatch(r -> r.direccion().equals(accidente.getDireccion())
                    && r.fecha().equals(accidente.getFecha() != null ? accidente.getFecha().toString() : "fecha desconocida")
                    && r.tipo().equals(accidente.getTipo()))) {
                continue; // Ya existe un registro con la misma dirección, fecha y tipo
            }
            registros.add(new AccidentRecord(
                    accidente.getFecha() != null ? accidente.getFecha().toString() : "fecha desconocida",
                    accidente.getDireccion(),
                    accidente.getTipo(),
                    embedding
            ));
        }
    }

    private void generarYGuardarEmbeddings(List<String[]> filas, List<String> descripciones) {
        EmbeddingResponse respuesta = embeddingModel.call(new EmbeddingRequest(descripciones, null));
        var resultados = respuesta.getResults();

        for (int i = 0; i < filas.size(); i++) {
            String[] columnas = filas.get(i);
            float[] embedding = resultados.get(i).getOutput();

            registros.add(new AccidentRecord(
                    columnas[5],
                    columnas[7],
                    columnas[9],
                    embedding
            ));
        }
    }

    public List<AccidentRecord> buscarSimilares(String direccion) {
        Assert.notNull(direccion, "La dirección no puede ser nula");

        EmbeddingResponse queryResponse = embeddingModel.call(new EmbeddingRequest(List.of(direccion),null));
        float[] queryEmbedding = queryResponse.getResults().get(0).getOutput();
        PriorityQueue<AccidentRecord> topSimilares =
                new PriorityQueue<>(TOP_K, Comparator.comparingDouble(
                        r -> cosineSimilarity(queryEmbedding, r.embedding())
                ));

        for (AccidentRecord registro : registros) {
            double simActual = cosineSimilarity(queryEmbedding, registro.embedding());

            if (topSimilares.size() < TOP_K) {
                topSimilares.offer(registro);
            } else {
                double simMin = cosineSimilarity(queryEmbedding, topSimilares.peek().embedding());
                if (simActual > simMin) {
                    topSimilares.poll();
                    topSimilares.offer(registro);
                }
            }
        }

        return topSimilares.stream()
                .sorted((a, b) -> Double.compare(
                        cosineSimilarity(queryEmbedding, b.embedding()),
                        cosineSimilarity(queryEmbedding, a.embedding())
                )).map(a -> new AccidentRecord(a.fecha(), a.direccion(), a.tipo(), null))
                .collect(Collectors.toList());
    }
    private double cosineSimilarity(float[] v1, float[] v2) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public String generarAlerta(String direccion) {
        List<AccidentRecord> similares = this.buscarSimilares(direccion);

        if (similares.isEmpty()) {
            return "No se encontraron registros de accidentes cercanos a esa zona.";
        }

        String datasetResumen = similares.stream()
                .map(a -> String.format("📅 %s | 📍 %s | 🚗 %s",
                        a.fecha(), a.direccion(), a.tipo()))
                .collect(Collectors.joining("\n"));

        String prompt = """
            Eres un asistente experto en seguridad vial en Bogotá.
            Tu tarea es analizar las zonas con mayor historial de accidentes a partir de las 10 direcciones más similares a la dirección dada: "%s".
            Los datos indican que las zonas con más accidentes relacionados son: %s.
            Con base en esta información:
            Indica qué zonas, vías o intersecciones debería evitar el conductor.
            Sugiere rutas o alternativas más seguras para llegar al destino.
            Da consejos breves y útiles para conducir con precaución en Bogotá, considerando el contexto vial de la ciudad.
            Requisitos de estilo:
            La respuesta debe ser muy resumida (máx. 3-4 oraciones).
            Usa un tono natural, empático y humano, como si hablaras directamente con un conductor.
            Evita tecnicismos innecesarios.
            Si no hay datos suficientes, responde con una recomendación general prudente.
            """.formatted(direccion, datasetResumen);

        var userMessage = new UserMessage(prompt);
        ChatResponse response = chatModel.call(new Prompt(List.of(userMessage),
                OpenAiChatOptions.builder()
                        .model(OpenAiApi.ChatModel.GPT_3_5_TURBO)
                        .outputModalities(List.of("text"))
                        .temperature(0.001)
                        .maxTokens(300)
                        .build()));
        return response.getResult().getOutput().getText();
    }
}
