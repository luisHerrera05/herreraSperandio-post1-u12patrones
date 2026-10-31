package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Arma la cadena en orden de costo creciente (fail-fast): documentos -> monto -> duplicados.
 * Ver "Decision de diseno 1" en el README. Agregar una verificacion nueva = un eslabon nuevo aqui.
 */
@Component
public class CadenaValidadores {

    private final ValidadorSolicitud primero;

    public CadenaValidadores(ValidadorDocumentosSoporte documentos,
                             ValidadorMontoMaximo monto,
                             ValidadorDuplicados duplicados) {
        documentos.siguiente(monto).siguiente(duplicados);
        this.primero = documentos;
    }

    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        return primero.validar(solicitud, pendientes);
    }
}
