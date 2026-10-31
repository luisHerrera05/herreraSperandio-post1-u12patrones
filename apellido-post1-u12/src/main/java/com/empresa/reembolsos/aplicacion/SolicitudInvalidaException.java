package com.empresa.reembolsos.aplicacion;

/** La solicitud no paso la cadena de validacion; el mensaje es el motivo del primer eslabon que rechazo. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String motivo) {
        super(motivo);
    }
}
