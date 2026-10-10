package com.esan.loteria.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esan.loteria.domain.Constantes
import com.esan.loteria.domain.Resultado

private val Verde = Color(0xFF2E7D32)
private val Rojo = Color(0xFFC62828)

@Composable
fun Bolita(numero: Int, resaltada: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (resaltada) Verde else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (resaltada) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, if (resaltada) Verde else MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(48.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numero.toString(), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeguimientoScreen(volver: () -> Unit, vm: SeguimientoViewModel = viewModel()) {
    // collectAsStateWithLifecycle suscribe solo mientras la pantalla está visible (STARTED):
    // al salir se cancelan los listeners de Firestore y al volver se registran de nuevo (sin duplicados).
    val ui by vm.ui.collectAsStateWithLifecycle()
    val progreso = ui.progreso

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = volver) { Text("← Volver") }
        }

        ui.sorteo?.let {
            Text(it.nombre, style = MaterialTheme.typography.headlineSmall)
            Text("Fecha: ${it.fecha}  ·  Estado: ${it.estado.etiqueta}")
        }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (ui.cargando) Text("Cargando…")

        // Mensaje / resultado calculado por la app (no se guarda en Firestore).
        if (progreso != null) {
            val (texto, color) = when (progreso.resultado) {
                Resultado.Espera -> "Esperando el inicio del sorteo…" to MaterialTheme.colorScheme.onSurface
                Resultado.EnCurso -> "Sorteo en curso" to MaterialTheme.colorScheme.primary
                Resultado.EsperandoResultados -> "Esperando resultados: faltan bolillas por publicar" to MaterialTheme.colorScheme.onSurface
                Resultado.Ganador -> "GANADOR" to Verde
                Resultado.SigueIntentando -> "SIGUE INTENTANDO" to Rojo
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(texto, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
                    Text("Bolillas publicadas: ${progreso.bolillasPublicadas} de ${Constantes.CANTIDAD_NUMEROS}")
                    Text("Aciertos: ${progreso.aciertos} de ${Constantes.CANTIDAD_NUMEROS}")
                }
            }
        }

        Text("Bolillas (orden de salida)", style = MaterialTheme.typography.titleMedium)
        if (ui.bolillas.isEmpty()) {
            Text("Aún no se publican bolillas.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.bolillas.forEach { b ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Bolita(b.numero, resaltada = b.numero in (ui.jugada?.numeros ?: emptyList()))
                        Text("#${b.orden}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Text("Tu jugada", style = MaterialTheme.typography.titleMedium)
        val jugada = ui.jugada
        if (jugada == null) {
            if (!ui.cargando) Text("No tienes una jugada registrada para este sorteo.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                jugada.numeros.forEach { Bolita(it, resaltada = progreso != null && it in progreso.numerosAcertados) }
            }
            Text("Los números en verde son aciertos.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
