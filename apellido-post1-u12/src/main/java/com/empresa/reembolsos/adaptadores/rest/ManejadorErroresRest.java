package com.empresa.reembolsos.adaptadores.rest;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudNoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Traduce las excepciones de la capa de aplicacion a respuestas HTTP con un mensaje claro. */
@RestControllerAdvice
public class ManejadorErroresRest {

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>> solicitudInvalida(SolicitudInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "Solicitud invalida", "mensaje", ex.getMessage()));
    }

    @ExceptionHandler(SolicitudNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> noEncontrada(SolicitudNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "No encontrada", "mensaje", ex.getMessage()));
    }
}
