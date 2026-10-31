package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Interesado en los cambios de estado de una solicitud (patron Observer).
 * El servicio notifica a todos los observadores registrados sin conocer sus detalles;
 * cada uno decide si le interesa el cambio y como reacciona.
 */
public interface ObservadorCambioEstado {

    /**
     * @param solicitud      solicitud ya con su nuevo estado
     * @param estadoAnterior estado previo al cambio
     */
    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}
