package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.util.List;

/**
 * Eslabon de la cadena de validacion (patron Chain of Responsibility).
 * Cada eslabon ejecuta su propia verificacion y, si la solicitud es valida, delega en el siguiente
 * sin saber cuantos eslabones hay despues. Si rechaza, la cadena se corta y se devuelve ese motivo.
 */
public abstract class ValidadorSolicitud {

    private ValidadorSolicitud siguiente;

    /** Registra el siguiente eslabon y lo devuelve, para poder encadenar llamadas. */
    public ValidadorSolicitud siguiente(ValidadorSolicitud siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado) {
        ResultadoValidacion propio = validarPropio(solicitud, pendientesDelEmpleado);
        if (!propio.valida() || siguiente == null) {
            return propio;
        }
        return siguiente.validar(solicitud, pendientesDelEmpleado);
    }

    protected abstract ResultadoValidacion validarPropio(
        SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado);
}
