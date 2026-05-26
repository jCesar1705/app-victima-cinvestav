# App Víctima — Sistema de emergencias sísmicas (CINVESTAV)

Prototipo de la **aplicación de usuario/víctima** (Android nativo, Kotlin) del proyecto
*Arquitectura de software de referencia para administración de casos de emergencia*.

Esta entrega cubre el **esqueleto en capas + CU-08 (recepción de alerta sísmica)**.

> ⚠️ Estos archivos **no se compilaron** en un entorno con SDK de Android; revísalos al
> importarlos en Android Studio. Están escritos para AGP/Kotlin/Compose actuales.

---

## 1. Cómo montarlo en Android Studio

La forma más robusta (deja que Android Studio genere el baseline correcto para tus
versiones instaladas):

1. **New Project → Empty Activity (Compose)**.
   - Package name: `mx.cinvestav.emergencias.victima`
   - Minimum SDK: **API 34 (Android 14)** — cumple R1 (últimas 3 versiones: Android 14/15/16).
2. Borra el `MainActivity.kt` y el tema generados que no uses, y **copia la carpeta
   `app/src/main/java/mx/...` de este ZIP** dentro de `app/src/main/java/`.
3. Agrega las dependencias (sección 2) y los permisos al manifest (sección 3).
4. En `AndroidManifest.xml`, en `<application>`, añade `android:name=".App"`.
5. Sincroniza Gradle y ejecuta.

Al abrir, con `USE_FAKE = true` (por defecto) verás la pantalla y podrás **disparar una
alerta simulada** con el botón — sin necesidad del nodo FOG.

---

## 2. Dependencias (`app/build.gradle.kts`)

Las versiones de Compose/AGP/Kotlin ya las fija el proyecto Empty Activity. Solo añade:

```kotlin
dependencies {
    // MQTT (cliente moderno; Paho Android Service está sin mantenimiento)
    implementation("com.hivemq:hivemq-mqtt-client:1.3.7")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Lifecycle + Compose (ViewModel y collectAsStateWithLifecycle)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
```

Si al compilar HiveMQ marca métodos de `java.util.concurrent` no disponibles, activa
*core library desugaring* en `compileOptions`/`kotlinOptions` (en API 34+ casi nunca
hace falta). Deja que Android Studio sugiera las versiones estables más recientes.

---

## 3. Permisos (`AndroidManifest.xml`)

Para esta iteración (CU-08):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 4. Arquitectura (mapeo PDF → Android)

```
ui/        → Capa de Presentación   (Compose + ViewModel + UiState)
domain/    → Capas de Aplicación y Dominio (casos de uso, modelos, contratos)
data/      → Capa de Datos          (fuente fake / fuente MQTT real, intercambiables)
platform/  → utilidades de Android  (notificaciones)
di/        → AppContainer (inyección manual + bandera fake/real)
```

La UI y los casos de uso dependen **solo de la interfaz `AlertaRepository`**. Por eso
podemos desarrollar en paralelo al nodo FOG: hoy usamos `FakeAlertaRepository`; cuando
el nodo exista, cambiamos `AppContainer.USE_FAKE = false` y se usa `MqttAlertaRepository`
sin tocar la UI ni los casos de uso.

---

## 5. Cómo cambiar de fuente fake → real

En `di/AppContainer.kt`:

```kotlin
const val USE_FAKE = false            // ⬅️ usar el nodo FOG real
const val FOG_HOST = "192.168.1.50"   // IP del nodo FOG (Raspberry Pi)
const val FOG_PUERTO = 1883
const val EDIFICIO_ID = "edificioA"
```

Para **probar el MQTT real sin el nodo todavía**, deja `FOG_HOST = "broker.hivemq.com"`
(broker público de pruebas) y publica un mensaje de prueba en otra terminal:

```bash
# Alerta
mosquitto_pub -h broker.hivemq.com -t "cinvestav/edificioA/alertas" \
  -m '{"id":"a1","tipo":"SISMICA","severidad":"ALTA","mensaje":"Alerta sísmica de prueba","timestamp":1716240000000}'

# Fin de emergencia (bloqueo automático)
mosquitto_pub -h broker.hivemq.com -t "cinvestav/edificioA/estado" \
  -m '{"modo":"NORMAL","timestamp":1716240500000}'
```

---

## 6. CONTRATO DE INTEGRACIÓN (acuerdo entre App ↔ Nodo FOG ↔ App Brigadista)

Este es el punto de coordinación con el equipo del nodo. **Lo que se defina aquí debe
respetarse en los tres componentes.**

### 6.1 MQTT (lo que la víctima *consume* en CU-08)

| Tópico | Dirección | Payload |
|---|---|---|
| `cinvestav/{edificioId}/alertas` | FOG → víctima | `AlertaDto` |
| `cinvestav/{edificioId}/estado`  | FOG → víctima | `EstadoDto` (publicar **retained**) |

```jsonc
// AlertaDto  (tópico .../alertas)
{
  "id": "uuid",            // único, para control de duplicados
  "tipo": "SISMICA",       // SISMICA | SIMULACRO
  "severidad": "ALTA",     // BAJA | MEDIA | ALTA  (opcional)
  "mensaje": "texto a mostrar",
  "timestamp": 1716240000000
}

// EstadoDto  (tópico .../estado, retained)
{
  "modo": "EMERGENCIA",    // NORMAL | EMERGENCIA
  "timestamp": 1716240000000
}
```

> El `estado` retained permite que el bloqueo automático (QA-10 / R7) funcione: al
> finalizar la emergencia, el FOG publica `{"modo":"NORMAL"}` y la app bloquea sola.

### 6.2 REST (lo que la víctima *enviará* en próximas iteraciones)

Definido aquí para que el nodo lo exponga; aún no implementado en la app.

| Método | Endpoint | CU | Cuerpo |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | — | credenciales → devuelve JWT |
| `POST` | `/api/v1/ubicacion` | CU-02 | `{victimaId, edificioId, fingerprint[], ...}` |
| `POST` | `/api/v1/salud` | CU-03 | datos médicos + consentimiento (R8) |
| `POST` | `/api/v1/reportes` | CU-04 | `{userId, ubicacion, tipo, descripcion, fecha}` |
| `GET`  | `/api/v1/edificios/{id}/mapa` | CU-01 | plano + zonas seguras (cacheable, QA-15) |

- Transporte: **HTTPS** (R3). Autenticación: **JWT** Bearer.
- Datos médicos: la app los envía en claro sobre TLS; **el nodo FOG cifra con AES en
  reposo** (no la app), conforme a la sección 3.4 del documento.

---

## 7. Validación CU-08 en esta entrega

| Driver | Mecanismo |
|---|---|
| CU-08 | Suscripción al tópico `alertas`; transición a modo EMERGENCIA |
| QA-03 | Entrega vía MQTT (publish/subscribe, baja latencia) |
| QA-10 / R7 | Modo controlado por el FOG; al volver a NORMAL se limpia la alerta (bloqueo automático) |

---

## 8. Siguientes pasos (orden propuesto)

1. **CU-03** — cuestionario de salud + consentimiento LFPDPPP (REST + Room + R8).
2. **CU-02** — envío de ubicación: Wi-Fi Fingerprinting + BLE + GPS.
3. **CU-01 / CU-04** — zonas seguras (mapa online/offline) y reporte en ≤4 interacciones.

**Pendiente arquitectónico anotado:** recepción de la alerta con la app cerrada
(proceso terminado) requiere un *foreground service* endurecido o FCM. En esta iteración
la alerta llega mientras el proceso vive (primer plano / segundo plano reciente).
