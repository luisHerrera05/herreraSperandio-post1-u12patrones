package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.procesamiento.SelectorCalculadora;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadorasTest {

    private SolicitudReembolso solicitud(String monto, int dias) {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setMontoSolicitado(new BigDecimal(monto));
        s.setDiasViaje(dias);
        return s;
    }

    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void viaje_montoBajoElTopeDiarioSeReembolsaCompleto() {
        assertMonto("200000", new CalculadoraViaje().calcular(solicitud("200000", 2)));
    }

    @Test
    void viaje_montoSobreElTopeDiarioTotalSeRecorta() {
        // 2 dias * 150000 = 300000
        assertMonto("300000", new CalculadoraViaje().calcular(solicitud("500000", 2)));
    }

    @Test
    void representacion_reembolsaCompletoHastaElTopeMensual() {
        assertMonto("1500000", new CalculadoraRepresentacion().calcular(solicitud("1500000", 0)));
        assertMonto("2000000", new CalculadoraRepresentacion().calcular(solicitud("2500000", 0)));
    }

    @Test
    void capacitacion_reembolsa80PorCientoHastaElTopeAnual() {
        assertMonto("80000", new CalculadoraCapacitacion().calcular(solicitud("100000", 0)));
        assertMonto("3000000", new CalculadoraCapacitacion().calcular(solicitud("5000000", 0)));
    }

    @Test
    void selector_devuelveLaEstrategiaDeCadaCategoria() {
        var viaje = new CalculadoraViaje();
        var repr = new CalculadoraRepresentacion();
        var cap = new CalculadoraCapacitacion();
        var selector = new SelectorCalculadora(List.of(viaje, repr, cap));
        assertSame(viaje, selector.obtener(CategoriaGasto.VIAJE));
        assertSame(repr, selector.obtener(CategoriaGasto.REPRESENTACION));
        assertSame(cap, selector.obtener(CategoriaGasto.CAPACITACION));
    }

    @Test
    void selector_categoriaSinEstrategiaLanzaExcepcion() {
        var selector = new SelectorCalculadora(List.of(new CalculadoraViaje()));
        assertThrows(IllegalArgumentException.class, () -> selector.obtener(CategoriaGasto.CAPACITACION));
    }
}
