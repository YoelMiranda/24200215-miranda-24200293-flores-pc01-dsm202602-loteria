package com.esan.loteria.domain

/** Resultado que la app muestra; NUNCA se guarda en Firestore. */
sealed interface Resultado {
    data object Espera : Resultado               // sorteo abierto
    data object EnCurso : Resultado              // sorteo en curso
    data object EsperandoResultados : Resultado  // finalizado pero faltan bolillas
    data object Ganador : Resultado
    data object SigueIntentando : Resultado
}

data class Progreso(
    val bolillasPublicadas: Int,
    val aciertos: Int,
    val numerosAcertados: Set<Int>,
    val resultado: Resultado,
)

object Calculo {

    /** Valida la jugada: exactamente 6 enteros distintos entre 1 y 45. Devuelve el mensaje de error o null. */
    fun validarJugada(numeros: List<Int>): String? = when {
        numeros.size != Constantes.CANTIDAD_NUMEROS ->
            "Debes elegir exactamente ${Constantes.CANTIDAD_NUMEROS} números."
        numeros.toSet().size != numeros.size -> "Los números no pueden repetirse."
        numeros.any { it !in Constantes.MIN_NUMERO..Constantes.MAX_NUMERO } ->
            "Los números deben estar entre ${Constantes.MIN_NUMERO} y ${Constantes.MAX_NUMERO}."
        else -> null
    }

    /**
     * Aciertos = números distintos de la jugada presentes entre las bolillas publicadas
     * (el orden no importa). El resultado final solo existe con sorteo finalizado y 6 bolillas.
     */
    fun calcular(jugada: List<Int>, bolillas: List<Bolilla>, estado: EstadoSorteo): Progreso {
        val salidos = bolillas.map { it.numero }.toSet()
        val acertados = jugada.toSet().intersect(salidos)
        val resultado = when (estado) {
            EstadoSorteo.ABIERTO -> Resultado.Espera
            EstadoSorteo.EN_CURSO -> Resultado.EnCurso
            EstadoSorteo.FINALIZADO ->
                if (salidos.size < Constantes.CANTIDAD_NUMEROS) Resultado.EsperandoResultados
                else if (acertados.size == Constantes.CANTIDAD_NUMEROS) Resultado.Ganador
                else Resultado.SigueIntentando
        }
        return Progreso(salidos.size, acertados.size, acertados, resultado)
    }
}
