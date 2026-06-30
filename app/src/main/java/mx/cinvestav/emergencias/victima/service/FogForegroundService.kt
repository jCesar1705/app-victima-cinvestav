package mx.cinvestav.emergencias.victima.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import mx.cinvestav.emergencias.victima.data.FogPreferences
import mx.cinvestav.emergencias.victima.ui.MainActivity
import org.json.JSONObject
import java.util.*

/**
 * Servicio en primer plano — Sprint 2 + Sprint 3.
 *
 * Sprint 2: heartbeats MQTT + alertas sísmicas en segundo plano
 * Sprint 3: envío de fingerprint WiFi + piso al FOG durante emergencia activa (CU-02)
 */
class FogForegroundService : Service() {

    companion object {
        const val CHANNEL_ID        = "fog_servicio"
        const val NOTIF_ID_SERVICIO = 1
        const val NOTIF_ID_ALERTA   = 2
        const val TAG               = "FogForegroundService"
        const val ACTION_ALERTA     = "mx.cinvestav.emergencias.ACTION_ALERTA"
        const val EDIFICIO_ID       = "edificioA"
    }

    private val scope      = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs: FogPreferences
    private lateinit var localizacion: LocalizacionManager
    private var mqttClient: Mqtt3AsyncClient? = null

    /** Controla si estamos en modo emergencia (Sprint 3). */
    @Volatile private var emergenciaActiva = false

    override fun onCreate() {
        super.onCreate()
        prefs = FogPreferences(applicationContext)
        localizacion = LocalizacionManager(applicationContext)
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID_SERVICIO, notificacionServicio())

        // Iniciar sensor barométrico para estimación de piso
        localizacion.iniciarBarometro()

        scope.launch { iniciarMqtt() }
        return START_STICKY
    }

    // ── MQTT ─────────────────────────────────────────────────────────────

    private suspend fun iniciarMqtt() {
        val host      = prefs.fogHost.first()
        val mqttPort  = prefs.mqttPort.first()
        val victimaId = prefs.userIdentificador.first().takeIf { it.isNotEmpty() } ?: "victima-002"

        if (host.isEmpty()) {
            Log.w(TAG, "No hay host configurado")
            return
        }

        try {
            mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(host)
                .serverPort(mqttPort)
                .identifier("victima-app-${UUID.randomUUID()}")
                .buildAsync()

            mqttClient?.connect()?.get()
            Log.i(TAG, "MQTT conectado a $host:$mqttPort")

            // Suscribir a alertas
            mqttClient?.subscribeWith()
                ?.topicFilter("cinvestav/$EDIFICIO_ID/alertas")
                ?.callback { publish -> onMensajeMqtt(publish, victimaId) }
                ?.send()

            // Heartbeat via MQTT (Sprint 2) + ubicación si hay emergencia (Sprint 3)
            scope.launch { bucleHeartbeatYUbicacion(victimaId) }

        } catch (e: Exception) {
            Log.e(TAG, "Error conectando MQTT: ${e.message}")
        }
    }

    // ── Heartbeat + Ubicación ─────────────────────────────────────────

    private suspend fun bucleHeartbeatYUbicacion(victimaId: String) {
        while (scope.isActive) {
            val host = prefs.fogHost.first()
            val port = prefs.fogPort.first()

            // Heartbeat MQTT (Sprint 2)
            enviarHeartbeatMqtt(victimaId)

            // Ubicación fingerprint WiFi (Sprint 3) — solo durante emergencia activa
            if (emergenciaActiva) {
                localizacion.enviarUbicacion(victimaId, "http://$host:$port")
            }

            delay(30_000)
        }
    }

    private fun enviarHeartbeatMqtt(victimaId: String) {
        try {
            if (mqttClient?.state?.isConnected == true) {
                val payload = """{"victimaId":"$victimaId","edificioId":"$EDIFICIO_ID","timestamp":${System.currentTimeMillis()}}"""
                mqttClient?.publishWith()
                    ?.topic("cinvestav/$EDIFICIO_ID/heartbeats")
                    ?.payload(payload.toByteArray())
                    ?.send()
                Log.d(TAG, "Heartbeat enviado como $victimaId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Heartbeat fallido: ${e.message}")
        }
    }

    // ── Mensajes MQTT entrantes ────────────────────────────────────────

    private fun onMensajeMqtt(publish: Mqtt3Publish, victimaId: String) {
        val payload = String(publish.payloadAsBytes)
        Log.i(TAG, "MQTT recibido: $payload")

        try {
            val json = JSONObject(payload)
            when (json.optString("tipo", "")) {

                "SISMICA", "SIMULACRO" -> {
                    emergenciaActiva = true   // ← Sprint 3: activar envío de ubicación
                    val mensaje   = json.optString("mensaje", "Alerta sísmica")
                    val severidad = json.optString("severidad", "ALTA")
                    mostrarAlerta(mensaje, severidad)
                    Log.i(TAG, "Emergencia activa — iniciando envío de ubicación")
                }

                "FIN_EMERGENCIA" -> {
                    emergenciaActiva = false  // ← Sprint 3: detener envío de ubicación
                    cancelarNotificacionAlerta()
                    Log.i(TAG, "Emergencia finalizada — deteniendo envío de ubicación")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje MQTT: ${e.message}")
        }
    }

    // ── Notificaciones ────────────────────────────────────────────────

    private fun mostrarAlerta(mensaje: String, severidad: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_ALERTA
            flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("mensaje", mensaje)
            putExtra("severidad", severidad)
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ ALERTA SÍSMICA — $severidad")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID_ALERTA, notif)
    }

    private fun cancelarNotificacionAlerta() {
        getSystemService(NotificationManager::class.java).cancel(NOTIF_ID_ALERTA)
    }

    private fun notificacionServicio(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SAES activo")
            .setContentText("Monitoreando alertas sísmicas en segundo plano")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(intent)
            .build()
    }

    private fun crearCanalNotificacion() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Alertas sísmicas SAES",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notificaciones de alertas sísmicas y estado del servicio" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        localizacion.detenerBarometro()
        scope.cancel()
        mqttClient?.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}