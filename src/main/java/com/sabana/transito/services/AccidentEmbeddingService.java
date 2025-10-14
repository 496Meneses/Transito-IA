package com.sabana.transito.services;

import com.sabana.transito.model.AccidentRecord;
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

    private static final int TOP_K = 10;
    private static final int LIMITE_DATASET = 100;

    public AccidentEmbeddingService(OpenAiEmbeddingModel modeloEmbeddings, ChatModel chatModel) throws Exception {
        this.embeddingModel = modeloEmbeddings;
        this.chatModel = chatModel;
        cargarYProcesarDataset();
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
            String direccion = columnas[7].trim();
            String gravedad = columnas[8].trim();
            String clase = columnas[9].trim();
            String localidad = columnas[10].trim();

            String descripcion = clase + " con " + gravedad + " en " + localidad + ", " + direccion;
            descripciones.add(descripcion);
        }
        return descripciones;
    }
    private void generarYGuardarEmbeddings(List<String[]> filas, List<String> descripciones) {
        EmbeddingResponse respuesta = embeddingModel.call(new EmbeddingRequest(descripciones, null));
        var resultados = respuesta.getResults();

        for (int i = 0; i < filas.size(); i++) {
            String[] columnas = filas.get(i);
            float[] embedding = resultados.get(i).getOutput();

            registros.add(new AccidentRecord(
                    columnas[5],   // FECHA
                    columnas[7],   // DIRECCIÓN
                    columnas[9],   // TIPO
                    embedding
            ));
        }
    }

    public List<AccidentRecord> buscarSimilares(String direccion) {
        Assert.notNull(direccion, "La dirección no puede ser nula");

        // Obtener embedding de la dirección consultada
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
            return "No se encontraron registros de accidentes cercanos a esa zona. ¡Buen viaje!";
        }

        String datasetResumen = similares.stream()
                .map(a -> String.format("📅 %s | 📍 %s | 🚗 %s",
                        a.fecha(), a.direccion(), a.tipo()))
                .collect(Collectors.joining("\n"));

        String prompt = """
            Eres un asistente experto en seguridad vial en Bogotá.
            Analiza las zonas con más accidentes reportados y da recomendaciones útiles.
            
            Las zonas donde han ocurrido más accidentes similares a "%s" son:
            %s
            
            Con base en esto:
            1. Menciona qué zonas o intersecciones debe evitar el conductor.
            2. Sugiere rutas o vías alternativas más seguras para desplazarse.
            3. Evita consejos genéricos; sé concreto con los nombres de las vías.
            4. Redacta el mensaje como si fuera una alerta de tráfico real.
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
