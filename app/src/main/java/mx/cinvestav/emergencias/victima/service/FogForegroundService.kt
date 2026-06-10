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
 * Servicio en primer plano — mantiene la conexión al broker MQTT
 * y publica heartbeats periódicos para registrar presencia en el nodo FOG.
 *
 * Heartbeat: MQTT topic cinvestav/{edificio}/heartbeats (Sprint 1)
 * Alertas:   MQTT topic cinvestav/{edificio}/alertas   (Sprint 1)
 */
class FogForegroundService : Service() {

    companion object {
        const val CHANNEL_ID        = "fog_servicio"
        const val NOTIF_ID_SERVICIO = 1
        const val NOTIF_ID_ALERTA   = 2
        const val TAG               = "FogForegroundService"
        const val ACTION_ALERTA     = "mx.cinvestav.emergencias.ACTION_ALERTA"
        const val EDIFICIO_ID       = "edificioA"   // debe coincidir con fog.node-id del servidor
    }

    private val scope      = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs: FogPreferences
    private var mqttClient: Mqtt3AsyncClient? = null

    override fun onCreate() {
        super.onCreate()
        prefs = FogPreferences(applicationContext)
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID_SERVICIO, notificacionServicio())
        scope.launch { iniciarMqtt() }
        return START_STICKY
    }

    // ── MQTT ─────────────────────────────────────────────────────────────

    private suspend fun iniciarMqtt() {
        val host     = prefs.fogHost.first()
        val mqttPort = prefs.mqttPort.first()
        val victimaId = prefs.userIdentificador.first().takeIf { it.isNotEmpty() } ?: "victima-002"

        if (host.isEmpty()) {
            Log.w(TAG, "No hay host configurado, no se inicia MQTT")
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

            // Suscribir a alertas del edificio
            mqttClient?.subscribeWith()
                ?.topicFilter("cinvestav/$EDIFICIO_ID/alertas")
                ?.callback { publish -> onMensajeMqtt(publish) }
                ?.send()

            Log.i(TAG, "Suscrito a cinvestav/$EDIFICIO_ID/alertas")

            // Iniciar heartbeats via MQTT (así es como lo espera el fog-node Sprint 1)
            iniciarHeartbeatsMqtt(victimaId)

        } catch (e: Exception) {
            Log.e(TAG, "Error conectando MQTT: ${e.message}")
        }
    }

    private suspend fun iniciarHeartbeatsMqtt(victimaId: String) {
        while (scope.isActive) {
            try {
                if (mqttClient?.state?.isConnected == true) {
                    // Payload que espera HeartbeatService del fog-node Sprint 1
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
            delay(30_000)
        }
    }

    // ── Mensajes MQTT entrantes ────────────────────────────────────────

    private fun onMensajeMqtt(publish: Mqtt3Publish) {
        val payload = String(publish.payloadAsBytes)
        Log.i(TAG, "MQTT recibido: $payload")

        try {
            val json = JSONObject(payload)
            // AlertaMensaje del Sprint 1: {id, tipo, severidad, mensaje, timestamp}
            val tipo = json.optString("tipo", "")

            when (tipo) {
                "SISMICA", "SIMULACRO" -> {
                    val mensaje   = json.optString("mensaje", "Alerta sísmica")
                    val severidad = json.optString("severidad", "ALTA")
                    mostrarAlerta(mensaje, severidad)
                }
                "FIN_EMERGENCIA" -> cancelarNotificacionAlerta()
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

        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_ALERTA, notif)
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
            CHANNEL_ID,
            "Alertas sísmicas SAES",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de alertas sísmicas y estado del servicio"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mqttClient?.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}