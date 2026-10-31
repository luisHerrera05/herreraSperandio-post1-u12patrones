package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;

/**
 * Estrategia de calculo del monto aprobado (patron Strategy).
 * Una implementacion por categoria de gasto; agregar una categoria nueva = una clase nueva anotada con @Component.
 */
public interface CalculadoraReembolso {

    /** Categoria de gasto que esta estrategia sabe calcular (llave de seleccion). */
    CategoriaGasto getCategoria();

    /** Monto aprobado segun la formula propia de la categoria. */
    BigDecimal calcular(SolicitudReembolso solicitud);
}
