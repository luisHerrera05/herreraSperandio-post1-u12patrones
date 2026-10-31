package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * Rechaza si el empleado ya tiene una solicitud PENDIENTE de la misma categoria en el mismo periodo (mes y anio).
 * No consulta la base de datos: recibe la lista ya cargada por el servicio, por eso este paquete
 * queda libre de dependencias de infraestructura.
 */
@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        YearMonth periodo = YearMonth.from(solicitud.getFechaGasto());
        boolean existeDuplicado = pendientes.stream()
            .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && YearMonth.from(p.getFechaGasto()).equals(periodo));
        return existeDuplicado
            ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
            : ResultadoValidacion.ok();
    }
}
