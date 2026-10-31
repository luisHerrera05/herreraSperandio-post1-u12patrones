package com.empresa.reembolsos.infraestructura;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Sin interfaz de puerto intermedia a proposito: ver docs/adr/ADR-001.md. */
public interface RepositorioSolicitudesJpa extends JpaRepository<SolicitudReembolso, Long> {

    List<SolicitudReembolso> findByEmpleadoIdAndEstado(String empleadoId, EstadoSolicitud estado);
}
