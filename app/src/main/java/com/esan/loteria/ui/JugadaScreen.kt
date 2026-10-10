package com.esan.loteria.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esan.loteria.data.AuthRepository
import com.esan.loteria.domain.Constantes
import com.esan.loteria.domain.EstadoSorteo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JugadaScreen(
    irASeguimiento: () -> Unit,
    cerrarSesion: () -> Unit,
    vm: JugadaViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val email = AuthRepository().email.orEmpty()
    val sorteo = ui.sorteo
    val abierto = sorteo?.estado == EstadoSorteo.ABIERTO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sesión iniciada como", style = MaterialTheme.typography.labelMedium)
                Text(email, style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = cerrarSesion) { Text("Cerrar sesión") }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                if (sorteo != null) {
                    Text(sorteo.nombre, style = MaterialTheme.typography.titleLarge)
                    Text("Fecha: ${sorteo.fecha}")
                    Text("Estado: ${sorteo.estado.etiqueta}")
                } else {
                    Text(if (ui.cargando) "Cargando sorteo…" else "No se encontró el sorteo.")
                }
            }
        }

        val jugada = ui.jugada
        if (jugada != null) {
            Text("Tu jugada registrada", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                jugada.numeros.forEach { Bolita(it, resaltada = false) }
            }
            Text("Las jugadas no se pueden modificar.", style = MaterialTheme.typography.bodySmall)
        } else if (sorteo != null && !abierto) {
            Text(
                "El sorteo ya inició: no se permite registrar jugadas.",
                color = MaterialTheme.colorScheme.error,
            )
        } else if (sorteo != null) {
            Text(
                "Elige ${Constantes.CANTIDAD_NUMEROS} números distintos del ${Constantes.MIN_NUMERO} al ${Constantes.MAX_NUMERO} " +
                    "(${ui.seleccion.size}/${Constantes.CANTIDAD_NUMEROS})",
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (Constantes.MIN_NUMERO..Constantes.MAX_NUMERO).forEach { n ->
                    FilterChip(
                        selected = n in ui.seleccion,
                        onClick = { vm.alternar(n) },
                        label = { Text(n.toString()) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::limpiar, enabled = ui.seleccion.isNotEmpty()) { Text("Limpiar") }
                Button(
                    onClick = vm::guardar,
                    enabled = !ui.guardando && ui.seleccion.size == Constantes.CANTIDAD_NUMEROS,
                ) { Text(if (ui.guardando) "Guardando…" else "Registrar jugada") }
            }
        }

        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = irASeguimiento,
            enabled = jugada != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Ir al seguimiento del sorteo") }
        if (jugada == null) {
            Text(
                "Registra tu jugada para poder seguir el sorteo.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
