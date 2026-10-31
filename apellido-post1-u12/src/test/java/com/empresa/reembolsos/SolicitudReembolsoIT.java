package com.empresa.reembolsos;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorAuditoria;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorContabilidad;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorEmpleado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de integracion del flujo completo: validar -> calcular -> persistir -> notificar. */
@SpringBootTest
@AutoConfigureMockMvc
class SolicitudReembolsoIT {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @SpyBean private NotificadorEmpleado empleado;
    @SpyBean private NotificadorContabilidad contabilidad;
    @SpyBean private NotificadorAuditoria auditoria;

    private String json(String empleado, String categoria, String monto, String fecha, int dias, boolean docs) {
        return String.format(
            "{\"empleadoId\":\"%s\",\"categoria\":\"%s\",\"montoSolicitado\":%s,\"fechaGasto\":\"%s\","
                + "\"diasViaje\":%d,\"documentosSoporte\":%s}", empleado, categoria, monto, fecha, dias, docs);
    }

    private long crear(String body) throws Exception {
        String resp = mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("id").asLong();
    }

    @Test
    void viaje_seCreaPendienteConMontoAprobado() throws Exception {
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                .content(json("IT-VIAJE", "VIAJE", "120000", "2026-08-10", 3, true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.montoAprobado", closeTo(120000.0, 0.01)));
    }

    @Test
    void representacion_seCreaConMontoCompletoDentroDelTope() throws Exception {
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                .content(json("IT-REPR", "REPRESENTACION", "1500000", "2026-08-10", 0, true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.montoAprobado", closeTo(1500000.0, 0.01)));
    }

    @Test
    void capacitacion_reembolsa80PorCiento() throws Exception {
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                .content(json("IT-CAP", "CAPACITACION", "100000", "2026-08-10", 0, true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.montoAprobado", closeTo(80000.0, 0.01)));
    }

    @Test
    void sinDocumentosSoporte_rechazaConMensajeClaro() throws Exception {
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                .content(json("IT-SINDOC", "VIAJE", "100000", "2026-08-10", 2, false)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje", containsString("documentos soporte")));
    }

    @Test
    void duplicadoPendienteMismaCategoriaYPeriodo_seRechaza() throws Exception {
        crear(json("IT-DUP", "VIAJE", "100000", "2026-08-05", 2, true));
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                .content(json("IT-DUP", "VIAJE", "90000", "2026-08-25", 2, true)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje", containsString("Ya existe")));
    }

    @Test
    void cambioAAprobada_notificaALosTresObservadores() throws Exception {
        long id = crear(json("IT-OBS", "REPRESENTACION", "800000", "2026-08-10", 0, true));

        mvc.perform(put("/api/solicitudes/" + id + "/estado")
                .contentType(MediaType.APPLICATION_JSON).content("\"APROBADA\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("APROBADA"));

        verify(empleado).onCambioEstado(any(SolicitudReembolso.class), eq(EstadoSolicitud.PENDIENTE));
        verify(contabilidad).onCambioEstado(any(SolicitudReembolso.class), eq(EstadoSolicitud.PENDIENTE));
        verify(auditoria).onCambioEstado(any(SolicitudReembolso.class), eq(EstadoSolicitud.PENDIENTE));
    }

    @Test
    void cambioDeEstadoDeSolicitudInexistente_devuelve404() throws Exception {
        mvc.perform(put("/api/solicitudes/999999/estado")
                .contentType(MediaType.APPLICATION_JSON).content("\"APROBADA\""))
            .andExpect(status().isNotFound());
    }
}
