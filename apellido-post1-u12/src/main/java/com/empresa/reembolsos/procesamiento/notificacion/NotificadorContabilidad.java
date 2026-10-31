package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Contabilidad solo reacciona a las APROBACIONES, para programar el pago. */
@Component
public class NotificadorContabilidad implements ObservadorCambioEstado {

    private static final Logger log = LoggerFactory.getLogger(NotificadorContabilidad.class);

    @Override
    public void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior) {
        if (solicitud.getEstado() == EstadoSolicitud.APROBADA) {
            log.info("Contabilidad: programar pago de {} para {}",
                     solicitud.getMontoAprobado(), solicitud.getEmpleadoId());
        }
    }
}
