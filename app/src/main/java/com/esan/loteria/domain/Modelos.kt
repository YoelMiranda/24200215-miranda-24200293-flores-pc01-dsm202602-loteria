package com.esan.loteria.domain

/** Estados del sorteo tal como se guardan en Firestore (campo `estado`). */
enum class EstadoSorteo(val valor: String, val etiqueta: String) {
    ABIERTO("abierto", "Abierto"),
    EN_CURSO("en_curso", "En curso"),
    FINALIZADO("finalizado", "Finalizado");

    companion object {
        fun desde(valor: String?): EstadoSorteo =
            entries.firstOrNull { it.valor == valor?.trim()?.lowercase() } ?: ABIERTO
    }
}

data class Sorteo(
    val id: String,
    val nombre: String,
    val fecha: String,
    val estado: EstadoSorteo,
)

data class Bolilla(val sorteoId: String, val numero: Int, val orden: Int)

data class Jugada(val sorteoId: String, val uid: String, val numeros: List<Int>)

object Constantes {
    /** Identificador del sorteo de prueba que usa la demostración. */
    const val SORTEO_ID = "sorteo-prueba"
    const val SORTEO_NOMBRE = "Sorteo de prueba"
    const val SORTEO_FECHA = "2026-02"
    const val CANTIDAD_NUMEROS = 6
    const val MIN_NUMERO = 1
    const val MAX_NUMERO = 45
}
