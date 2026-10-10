package com.esan.loteria

import com.esan.loteria.domain.Bolilla
import com.esan.loteria.domain.Calculo
import com.esan.loteria.domain.EstadoSorteo
import com.esan.loteria.domain.Resultado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalculoTest {
    private val salida = listOf(5, 12, 18, 23, 34, 41)
    private fun bolillas(n: Int) = salida.take(n).mapIndexed { i, v -> Bolilla("s", v, i + 1) }

    @Test fun usuarioA_gana() {
        val p = Calculo.calcular(listOf(5, 12, 18, 23, 34, 41), bolillas(6), EstadoSorteo.FINALIZADO)
        assertEquals(6, p.aciertos)
        assertEquals(Resultado.Ganador, p.resultado)
    }

    @Test fun usuarioB_sigueIntentando() {
        val p = Calculo.calcular(listOf(5, 12, 18, 23, 34, 42), bolillas(6), EstadoSorteo.FINALIZADO)
        assertEquals(5, p.aciertos)
        assertEquals(Resultado.SigueIntentando, p.resultado)
    }

    @Test fun quintaBolilla_sinResultadoDefinitivo() {
        val p = Calculo.calcular(listOf(5, 12, 18, 23, 34, 41), bolillas(5), EstadoSorteo.EN_CURSO)
        assertEquals(5, p.aciertos)
        assertEquals(5, p.bolillasPublicadas)
        assertEquals(Resultado.EnCurso, p.resultado)
    }

    @Test fun finalizadoSinSeisBolillas_esperaResultados() {
        val p = Calculo.calcular(listOf(5, 12, 18, 23, 34, 41), bolillas(5), EstadoSorteo.FINALIZADO)
        assertEquals(Resultado.EsperandoResultados, p.resultado)
    }

    @Test fun abierto_espera() {
        val p = Calculo.calcular(listOf(1, 2, 3, 4, 5, 6), emptyList(), EstadoSorteo.ABIERTO)
        assertEquals(Resultado.Espera, p.resultado)
    }

    @Test fun validaciones() {
        assertNull(Calculo.validarJugada(listOf(5, 12, 18, 23, 34, 41)))
        assertNotNull(Calculo.validarJugada(listOf(1, 2, 3)))
        assertNotNull(Calculo.validarJugada(listOf(1, 1, 2, 3, 4, 5)))
        assertNotNull(Calculo.validarJugada(listOf(0, 2, 3, 4, 5, 46)))
    }
}
