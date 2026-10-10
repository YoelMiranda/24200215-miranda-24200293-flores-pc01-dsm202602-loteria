# PC01 – Lotería (DSM 2026-02)

Aplicación Android (Kotlin + Jetpack Compose + Firebase Authentication + Cloud Firestore) para registrar una jugada de
seis números y seguir un sorteo en tiempo real.

## Integrantes

| Nombre | Código |
|--------|--------|
| Miranda | 24200215 |
| Flores | 24200293 |

> Completar los nombres completos antes de entregar.

## Instrucciones de ejecución

1. Abrir la carpeta del proyecto con Android Studio (JDK 17, AGP 8.7.2, Kotlin 2.0.21).
2. Crear un proyecto en [Firebase Console](https://console.firebase.google.com) y registrar la app Android con el
   paquete `com.esan.loteria`.
3. Activar **Authentication → Correo electrónico/contraseña** y crear la base **Cloud Firestore**.
4. Descargar `google-services.json` y copiarlo en `app/google-services.json`.
5. Publicar las reglas de [`firestore.rules`](firestore.rules) (pestaña *Reglas* de Firestore).
6. Ejecutar en dos emuladores/dispositivos con el mismo proyecto Firebase.

Pruebas unitarias del cálculo: `./gradlew testDebugUnitTest`.

## Modelo en Firestore

```
sorteos/{sorteoId}                 nombre, fecha, estado ("abierto" | "en_curso" | "finalizado")
sorteos/{sorteoId}/bolillas/{id}   sorteoId, numero (number), orden (number 1..6)
jugadas/{sorteoId}_{uid}           sorteoId, uid, numeros (array de number), creadaEn
```

El sorteo de la prueba es `sorteos/sorteo-prueba`; la app lo crea en estado `abierto` si no existe. Los textos
GANADOR / SIGUE INTENTANDO **no** se guardan: la app los calcula (`domain/Calculo.kt`).

## Guion de la demostración

1. Usuario A (emulador 1) registra `5, 12, 18, 23, 34, 41`; Usuario B (emulador 2) registra `5, 12, 18, 23, 34, 42`.
   Ambos abren el seguimiento.
2. En la consola, cambiar `sorteos/sorteo-prueba.estado` de `abierto` a `en_curso`.
3. Agregar, **una por una**, documentos en `sorteos/sorteo-prueba/bolillas` (ID automático):

   | orden | numero |
   |-------|--------|
   | 1 | 5 |
   | 2 | 12 |
   | 3 | 18 |
   | 4 | 23 |
   | 5 | 34 |
   | 6 | 41 |

   Campos `numero` y `orden` de tipo **number**; `sorteoId` (string) = `sorteo-prueba`.
4. Tras la 5.ª bolilla ambos muestran 5 aciertos y "Sorteo en curso". Tras la 6.ª, cambiar `estado` a `finalizado`:
   A → GANADOR, B → SIGUE INTENTANDO.
5. Cerrar y reabrir la app: la sesión, la jugada y las bolillas se recuperan y el resultado se recalcula.

## Dónde mirar el código

- Escucha en tiempo real: `data/SorteoRepository.kt` (`callbackFlow` + `addSnapshotListener`, con `awaitClose { reg.remove() }`).
- Combinación de listeners y ciclo de vida: `ui/ViewModels.kt` (`SeguimientoViewModel`, `WhileSubscribed(0)`) y
  `collectAsStateWithLifecycle` en `ui/SeguimientoScreen.kt`.
- Cálculos y validaciones: `domain/Calculo.kt` (+ `CalculoTest`).
- Autenticación: `data/AuthRepository.kt`.
- Registro de jugada (transacción, solo con sorteo abierto): `SorteoRepository.registrarJugada`.
