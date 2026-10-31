package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadoresTest {

    private SolicitudReembolso solicitud(CategoriaGasto cat, String monto, LocalDate fecha, boolean docs) {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setEmpleadoId("E1");
        s.setCategoria(cat);
        s.setMontoSolicitado(new BigDecimal(monto));
        s.setFechaGasto(fecha);
        s.setDocumentosSoporte(docs);
        return s;
    }

    // ---- ValidadorDocumentosSoporte ----
    @Test
    void documentos_conSoporte_esValida() {
        var r = new ValidadorDocumentosSoporte().validar(
            solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2026, 8, 1), true), List.of());
        assertTrue(r.valida());
    }

    @Test
    void documentos_sinSoporte_rechazaConMensajeClaro() {
        var r = new ValidadorDocumentosSoporte().validar(
            solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2026, 8, 1), false), List.of());
        assertFalse(r.valida());
        assertEquals("Faltan documentos soporte", r.motivoRechazo());
    }

    // ---- ValidadorMontoMaximo ----
    @Test
    void monto_dentroDelTope_esValida() {
        var r = new ValidadorMontoMaximo().validar(
            solicitud(CategoriaGasto.CAPACITACION, "3000000", LocalDate.of(2026, 8, 1), true), List.of());
        assertTrue(r.valida());
    }

    @Test
    void monto_excedeElTope_rechaza() {
        var r = new ValidadorMontoMaximo().validar(
            solicitud(CategoriaGasto.VIAJE, "150001", LocalDate.of(2026, 8, 1), true), List.of());
        assertFalse(r.valida());
        assertTrue(r.motivoRechazo().contains("VIAJE"));
    }

    // ---- validadorDuplicados ----
    @Test
    void duplicados_mismaCategoriaYPeriodo_rechaza() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "50000", LocalDate.of(2026, 8, 3), true);
        var nueva = solicitud(CategoriaGasto.VIAJE, "70000", LocalDate.of(2026, 8, 20), true);
        var r = new validadorDuplicados().validar(nueva, List.of(pendiente));
        assertFalse(r.valida());
    }

    @Test
    void duplicados_otraCategoriaOtroMesOtroAnio_esValida() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "50000", LocalDate.of(2026, 8, 3), true);
        var validadorDup = new validadorDuplicados();
        assertTrue(validadorDup.validar(
            solicitud(CategoriaGasto.CAPACITACION, "70000", LocalDate.of(2026, 8, 3), true), List.of(pendiente)).valida());
        assertTrue(validadorDup.validar(
            solicitud(CategoriaGasto.VIAJE, "70000", LocalDate.of(2026, 9, 3), true), List.of(pendiente)).valida());
        assertTrue(validadorDup.validar(
            solicitud(CategoriaGasto.VIAJE, "70000", LocalDate.of(2027, 8, 3), true), List.of(pendiente)).valida());
    }

    // ---- CadenaValidadores ----
    @Test
    void cadena_cortaEnElPrimerRechazoSegunElOrden() {
        var cadena = new CadenaValidadores(
            new ValidadorDocumentosSoporte(), new ValidadorMontoMaximo(), new validadorDuplicados());
        // sin documentos Y con monto excesivo: gana el primer eslabon (documentos)
        var r = cadena.validar(solicitud(CategoriaGasto.VIAJE, "999999", LocalDate.of(2026, 8, 1), false), List.of());
        assertEquals("Faltan documentos soporte", r.motivoRechazo());
    }

    @Test
    void cadena_solicitudCorrectaPasaLosTresEslabones() {
        var cadena = new CadenaValidadores(
            new ValidadorDocumentosSoporte(), new ValidadorMontoMaximo(), new validadorDuplicados());
        assertTrue(cadena.validar(solicitud(CategoriaGasto.REPRESENTACION, "500000", LocalDate.of(2026, 8, 1), true), List.of()).valida());
    }
}
