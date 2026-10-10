package com.esan.loteria.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esan.loteria.data.AuthRepository
import com.esan.loteria.data.SorteoRepository
import com.esan.loteria.domain.Bolilla
import com.esan.loteria.domain.Calculo
import com.esan.loteria.domain.Constantes
import com.esan.loteria.domain.Jugada
import com.esan.loteria.domain.Progreso
import com.esan.loteria.domain.Sorteo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de las pantallas de login y registro. */
data class AuthUi(val cargando: Boolean = false, val error: String? = null)

class AuthViewModel(private val auth: AuthRepository = AuthRepository()) : ViewModel() {
    private val _ui = MutableStateFlow(AuthUi())
    val ui: StateFlow<AuthUi> = _ui.asStateFlow()

    fun limpiarError() = _ui.update { it.copy(error = null) }

    fun iniciarSesion(email: String, clave: String, alExito: () -> Unit) =
        ejecutar(email, clave, alExito) { auth.iniciarSesion(email, clave) }

    fun registrar(email: String, clave: String, alExito: () -> Unit) =
        ejecutar(email, clave, alExito) { auth.registrar(email, clave) }

    private fun ejecutar(email: String, clave: String, alExito: () -> Unit, accion: suspend () -> Unit) {
        if (email.isBlank() || clave.isBlank()) {
            _ui.value = AuthUi(error = "Ingresa correo y contraseña.")
            return
        }
        _ui.value = AuthUi(cargando = true)
        viewModelScope.launch {
            try {
                accion()
                _ui.value = AuthUi()
                alExito()
            } catch (e: Exception) {
                _ui.value = AuthUi(error = AuthRepository.mensaje(e))
            }
        }
    }
}

/** Estado de la pantalla de registro de jugada. */
data class JugadaUi(
    val sorteo: Sorteo? = null,
    val jugada: Jugada? = null,
    val seleccion: Set<Int> = emptySet(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
)

class JugadaViewModel(
    private val repo: SorteoRepository = SorteoRepository(),
    private val auth: AuthRepository = AuthRepository(),
) : ViewModel() {
    private val _ui = MutableStateFlow(JugadaUi())
    val ui: StateFlow<JugadaUi> = _ui.asStateFlow()

    init {
        val uid = auth.uid
        viewModelScope.launch {
            try {
                repo.asegurarSorteoPrueba()
            } catch (_: Exception) {
                // Si falla (p. ej. sin red) se sigue escuchando; la UI mostrará que no hay sorteo.
            }
            if (uid == null) return@launch
            combine(
                repo.observarSorteo(Constantes.SORTEO_ID),
                repo.observarJugada(Constantes.SORTEO_ID, uid),
            ) { s, j -> s to j }
                .catch { e -> _ui.update { it.copy(cargando = false, error = e.message ?: "Error al leer datos.") } }
                .collect { (s, j) -> _ui.update { it.copy(sorteo = s, jugada = j, cargando = false) } }
        }
    }

    fun alternar(n: Int) = _ui.update {
        val nueva = when {
            n in it.seleccion -> it.seleccion - n
            it.seleccion.size < Constantes.CANTIDAD_NUMEROS -> it.seleccion + n
            else -> it.seleccion
        }
        it.copy(seleccion = nueva, error = null)
    }

    fun limpiar() = _ui.update { it.copy(seleccion = emptySet(), error = null) }

    fun guardar() {
        val uid = auth.uid ?: return
        val numeros = _ui.value.seleccion.sorted()
        val invalida = Calculo.validarJugada(numeros)
        if (invalida != null) {
            _ui.update { it.copy(error = invalida) }
            return
        }
        _ui.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            try {
                repo.registrarJugada(Constantes.SORTEO_ID, uid, numeros)
                _ui.update { it.copy(guardando = false) }
            } catch (e: Exception) {
                _ui.update { it.copy(guardando = false, error = e.message ?: "No se pudo guardar la jugada.") }
            }
        }
    }
}

/** Estado de la pantalla de seguimiento. */
data class SeguimientoUi(
    val cargando: Boolean = true,
    val sorteo: Sorteo? = null,
    val jugada: Jugada? = null,
    val bolillas: List<Bolilla> = emptyList(),
    val progreso: Progreso? = null,
    val error: String? = null,
)

class SeguimientoViewModel(
    repo: SorteoRepository = SorteoRepository(),
    auth: AuthRepository = AuthRepository(),
) : ViewModel() {

    /**
     * Combina los tres listeners de Firestore (sorteo, bolillas y jugada del usuario) y recalcula
     * aciertos/resultado en cada emisión. Con WhileSubscribed(0) los listeners se retiran en cuanto
     * la pantalla deja de observar (salir de la pantalla o pasar a segundo plano), sin duplicados.
     */
    val ui: StateFlow<SeguimientoUi> = construirFlujo(repo, auth.uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), SeguimientoUi())

    private fun construirFlujo(repo: SorteoRepository, uid: String?): Flow<SeguimientoUi> {
        if (uid == null) return flowOf(SeguimientoUi(cargando = false, error = "No hay sesión activa."))
        return combine(
            repo.observarSorteo(Constantes.SORTEO_ID),
            repo.observarBolillas(Constantes.SORTEO_ID),
            repo.observarJugada(Constantes.SORTEO_ID, uid),
        ) { sorteo, bolillas, jugada ->
            val progreso = if (sorteo != null && jugada != null) {
                Calculo.calcular(jugada.numeros, bolillas, sorteo.estado)
            } else null
            SeguimientoUi(false, sorteo, jugada, bolillas, progreso)
        }.catch { e ->
            emit(SeguimientoUi(cargando = false, error = e.message ?: "Error al escuchar cambios."))
        }
    }
}
