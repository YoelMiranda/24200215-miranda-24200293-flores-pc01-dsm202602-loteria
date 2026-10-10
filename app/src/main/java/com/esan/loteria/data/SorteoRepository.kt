package com.esan.loteria.data

import com.esan.loteria.domain.Bolilla
import com.esan.loteria.domain.Calculo
import com.esan.loteria.domain.Constantes
import com.esan.loteria.domain.EstadoSorteo
import com.esan.loteria.domain.Jugada
import com.esan.loteria.domain.Sorteo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Estructura en Firestore:
 *  - sorteos/{sorteoId}                 -> nombre, fecha, estado ("abierto" | "en_curso" | "finalizado")
 *  - sorteos/{sorteoId}/bolillas/{id}   -> sorteoId, numero, orden
 *  - jugadas/{sorteoId}_{uid}           -> sorteoId, uid, numeros[], creadaEn
 */
class SorteoRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val sorteos get() = db.collection("sorteos")
    private val jugadas get() = db.collection("jugadas")

    private fun idJugada(sorteoId: String, uid: String) = "${sorteoId}_$uid"

    /** Crea la cabecera del sorteo de prueba (abierto) si aún no existe. */
    suspend fun asegurarSorteoPrueba() {
        val ref = sorteos.document(Constantes.SORTEO_ID)
        val existe = runCatching { ref.get().await().exists() }.getOrDefault(true)
        if (!existe) {
            ref.set(
                mapOf(
                    "nombre" to Constantes.SORTEO_NOMBRE,
                    "fecha" to Constantes.SORTEO_FECHA,
                    "estado" to EstadoSorteo.ABIERTO.valor,
                )
            ).await()
        }
    }

    private fun DocumentSnapshot.aSorteo(): Sorteo? {
        if (!exists()) return null
        return Sorteo(
            id = id,
            nombre = getString("nombre") ?: id,
            fecha = getString("fecha") ?: "",
            estado = EstadoSorteo.desde(getString("estado")),
        )
    }

    /** Escucha en tiempo real la cabecera del sorteo. El listener se retira al cancelar el flujo. */
    fun observarSorteo(sorteoId: String): Flow<Sorteo?> = callbackFlow {
        val reg = sorteos.document(sorteoId).addSnapshotListener { snap, error ->
            if (error != null) close(error) else trySend(snap?.aSorteo())
        }
        awaitClose { reg.remove() }
    }

    /** Escucha en tiempo real las bolillas del sorteo, ordenadas por orden de salida. */
    fun observarBolillas(sorteoId: String): Flow<List<Bolilla>> = callbackFlow {
        val reg = sorteos.document(sorteoId).collection("bolillas")
            .orderBy("orden", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                } else {
                    val lista = snap?.documents?.mapNotNull { d ->
                        val numero = d.getLong("numero")?.toInt()
                        val orden = d.getLong("orden")?.toInt()
                        if (numero == null || orden == null) null else Bolilla(sorteoId, numero, orden)
                    }.orEmpty()
                    trySend(lista)
                }
            }
        awaitClose { reg.remove() }
    }

    /** Escucha la jugada del usuario (solo la suya: el id del documento incluye su uid). */
    fun observarJugada(sorteoId: String, uid: String): Flow<Jugada?> = callbackFlow {
        val reg = jugadas.document(idJugada(sorteoId, uid)).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
            } else {
                val nums = (snap?.get("numeros") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
                trySend(
                    if (snap?.exists() == true && nums != null) Jugada(sorteoId, uid, nums.sorted()) else null
                )
            }
        }
        awaitClose { reg.remove() }
    }

    /**
     * Registra la jugada en una transacción: solo si el sorteo está abierto y el usuario
     * aún no tiene jugada (una por usuario; no se modifica).
     */
    suspend fun registrarJugada(sorteoId: String, uid: String, numeros: List<Int>) {
        Calculo.validarJugada(numeros)?.let { throw IllegalArgumentException(it) }
        val refSorteo = sorteos.document(sorteoId)
        val refJugada = jugadas.document(idJugada(sorteoId, uid))
        db.runTransaction { tx ->
            val sorteo = tx.get(refSorteo)
            if (EstadoSorteo.desde(sorteo.getString("estado")) != EstadoSorteo.ABIERTO) {
                throw IllegalStateException("El sorteo ya inició: no se pueden registrar jugadas.")
            }
            if (tx.get(refJugada).exists()) {
                throw IllegalStateException("Ya registraste una jugada para este sorteo.")
            }
            tx.set(
                refJugada,
                mapOf(
                    "sorteoId" to sorteoId,
                    "uid" to uid,
                    "numeros" to numeros.sorted(),
                    "creadaEn" to FieldValue.serverTimestamp(),
                )
            )
            null
        }.await()
    }
}
