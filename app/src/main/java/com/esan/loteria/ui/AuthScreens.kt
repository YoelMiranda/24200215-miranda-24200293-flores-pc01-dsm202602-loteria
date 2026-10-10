package com.esan.loteria.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(alExito: () -> Unit, irARegistro: () -> Unit, vm: AuthViewModel = viewModel()) {
    FormularioAuth(
        titulo = "Iniciar sesión",
        accion = "Ingresar",
        alternativa = "¿No tienes cuenta? Regístrate",
        vm = vm,
        onAccion = { email, clave -> vm.iniciarSesion(email, clave, alExito) },
        onAlternativa = irARegistro,
    )
}

@Composable
fun RegistroScreen(alExito: () -> Unit, irALogin: () -> Unit, vm: AuthViewModel = viewModel()) {
    FormularioAuth(
        titulo = "Crear cuenta",
        accion = "Registrarme",
        alternativa = "¿Ya tienes cuenta? Inicia sesión",
        vm = vm,
        onAccion = { email, clave -> vm.registrar(email, clave, alExito) },
        onAlternativa = irALogin,
    )
}

@Composable
private fun FormularioAuth(
    titulo: String,
    accion: String,
    alternativa: String,
    vm: AuthViewModel,
    onAccion: (String, String) -> Unit,
    onAlternativa: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Lotería", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; vm.limpiarError() },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it; vm.limpiarError() },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        ui.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        if (ui.cargando) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { onAccion(email, clave) }, modifier = Modifier.fillMaxWidth()) { Text(accion) }
        }
        TextButton(onClick = onAlternativa, enabled = !ui.cargando) { Text(alternativa) }
    }
}
