package com.esan.loteria.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.esan.loteria.data.AuthRepository

object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val JUGADA = "jugada"
    const val SEGUIMIENTO = "seguimiento"
}

private val Esquema = lightColorScheme(
    primary = Color(0xFF1B5E20),
    secondary = Color(0xFFF9A825),
)

@Composable
fun LoteriaApp() {
    MaterialTheme(colorScheme = Esquema) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val nav = rememberNavController()
            val auth = AuthRepository()
            // La sesión de Firebase Auth persiste: si ya hay usuario se entra directo a la jugada.
            val inicio = if (auth.haySesion) Rutas.JUGADA else Rutas.LOGIN

            NavHost(navController = nav, startDestination = inicio) {
                composable(Rutas.LOGIN) {
                    LoginScreen(
                        alExito = {
                            nav.navigate(Rutas.JUGADA) { popUpTo(Rutas.LOGIN) { inclusive = true } }
                        },
                        irARegistro = { nav.navigate(Rutas.REGISTRO) },
                    )
                }
                composable(Rutas.REGISTRO) {
                    RegistroScreen(
                        alExito = {
                            nav.navigate(Rutas.JUGADA) { popUpTo(Rutas.LOGIN) { inclusive = true } }
                        },
                        irALogin = { nav.popBackStack() },
                    )
                }
                composable(Rutas.JUGADA) {
                    JugadaScreen(
                        irASeguimiento = { nav.navigate(Rutas.SEGUIMIENTO) },
                        cerrarSesion = {
                            auth.cerrarSesion()
                            nav.navigate(Rutas.LOGIN) { popUpTo(0) { inclusive = true } }
                        },
                    )
                }
                composable(Rutas.SEGUIMIENTO) {
                    SeguimientoScreen(volver = { nav.popBackStack() })
                }
            }
        }
    }
}
