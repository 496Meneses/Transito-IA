package com.sabana.transito.utilidades;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FechaUtil {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FechaUtil() {
        // Evita instanciación
    }

    public static LocalDate convertirStringALocalDate(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(fecha, FORMATO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha inválido. Se esperaba dd/MM/yyyy, recibido: " + fecha);
        }
    }
}

