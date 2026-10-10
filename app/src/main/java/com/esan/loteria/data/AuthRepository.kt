package com.esan.loteria.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val uid: String? get() = auth.currentUser?.uid
    val email: String? get() = auth.currentUser?.email
    val haySesion: Boolean get() = auth.currentUser != null

    suspend fun registrar(email: String, clave: String) {
        auth.createUserWithEmailAndPassword(email.trim(), clave).await()
    }

    suspend fun iniciarSesion(email: String, clave: String) {
        auth.signInWithEmailAndPassword(email.trim(), clave).await()
    }

    fun cerrarSesion() = auth.signOut()

    companion object {
        /** Traduce excepciones de Firebase Auth a mensajes comprensibles. */
        fun mensaje(e: Throwable): String = when (e) {
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil (mínimo 6 caracteres)."
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
            is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
            is FirebaseNetworkException -> "Sin conexión. Revisa tu internet."
            is FirebaseAuthException -> "No se pudo autenticar: ${e.errorCode}"
            else -> e.message ?: "Ocurrió un error inesperado."
        }
    }
}
