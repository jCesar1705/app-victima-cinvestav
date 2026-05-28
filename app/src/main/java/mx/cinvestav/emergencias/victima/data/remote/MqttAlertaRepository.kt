package mx.cinvestav.emergencias.victima.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.cinvestav.emergencias.victima.data.remote.mqtt.MqttClienteFog
import mx.cinvestav.emergencias.victima.data.remote.mqtt.MqttTopics
import mx.cinvestav.emergencias.victima.domain.AlertaRepository
import mx.cinvestav.emergencias.victima.domain.model.Alerta
import mx.cinvestav.emergencias.victima.domain.model.EstadoConexion
import mx.cinvestav.emergencias.victima.domain.model.EstadoEmergencia
import mx.cinvestav.emergencias.victima.domain.model.ModoSistema
import mx.cinvestav.emergencias.victima.domain.model.Severidad
import mx.cinvestav.emergencias.victima.domain.model.TipoAlerta

/**
 * Implementación real del repositorio de alertas vía MQTT.
 *
 * Además de recibir alertas y cambios de modo, publica heartbeats periódicos
 * al nodo FOG para que este sepa quién está presente (T4).
 *
 * @param victimaId      ID de la víctima — debe coincidir con los datos del nodo FOG
 * @param scope          scope de la aplicación para el loop de heartbeats
 * @param heartbeatMs    intervalo de heartbeat (30s para demo, 300s en producción)
 */
class MqttAlertaRepository(
    private val cliente: MqttClienteFog,
    private val edificioId: String,
    private val victimaId: String,
    private val scope: CoroutineScope,
    private val heartbeatMs: Long = 30_000L,
    private val gson: Gson = Gson()
) : AlertaRepository {

    override val estadoConexion: StateFlow<EstadoConexion> = cliente.estadoConexion

    private val _estadoEmergencia = MutableStateFlow(EstadoEmergencia())
    override val estadoEmergencia: StateFlow<EstadoEmergencia> = _estadoEmergencia.asStateFlow()

    private val _eventosAlerta = MutableSharedFlow<Alerta>(extraBufferCapacity = 8)
    override val eventosAlerta: Flow<Alerta> = _eventosAlerta.asSharedFlow()

    override fun conectar() {
        cliente.conectar {
            cliente.suscribir(MqttTopics.alertas(edificioId)) { _, payload -> manejarAlerta(payload) }
            cliente.suscribir(MqttTopics.estado(edificioId))  { _, payload -> manejarEstado(payload) }
            iniciarHeartbeat()
        }
    }

    override fun desconectar() = cliente.desconectar()

    // --- Heartbeat (T4) ---

    private fun iniciarHeartbeat() {
        scope.launch {
            while (true) {
                delay(heartbeatMs)
                if (cliente.estadoConexion.value == EstadoConexion.CONECTADO) {
                    val payload = gson.toJson(mapOf(
                        "victimaId"  to victimaId,
                        "edificioId" to edificioId,
                        "timestamp"  to System.currentTimeMillis()
                    ))
                    cliente.publicar(MqttTopics.heartbeats(edificioId), payload)
                    Log.d("MqttAlertaRepository", "Heartbeat enviado: $victimaId")
                }
            }
        }
    }

    // --- Manejo de mensajes ---

    private fun manejarAlerta(payload: String) {
        runCatching { gson.fromJson(payload, AlertaDto::class.java) }
            .onSuccess { dto ->
                if (dto.tipo.equals("FIN_EMERGENCIA", ignoreCase = true)) {
                    _estadoEmergencia.value = EstadoEmergencia(ModoSistema.NORMAL, null)
                    return@onSuccess
                }
                val alerta = dto.toDominio(edificioId)
                _estadoEmergencia.value = EstadoEmergencia(ModoSistema.EMERGENCIA, alerta)
                _eventosAlerta.tryEmit(alerta)
            }
            .onFailure { Log.e(TAG, "Payload de alerta inválido: $payload", it) }
    }

    private fun manejarEstado(payload: String) {
        runCatching { gson.fromJson(payload, EstadoDto::class.java) }
            .onSuccess { dto ->
                val modo = if (dto.modo.equals("EMERGENCIA", ignoreCase = true))
                    ModoSistema.EMERGENCIA else ModoSistema.NORMAL
                _estadoEmergencia.value = _estadoEmergencia.value.copy(
                    modo = modo,
                    alertaActiva = if (modo == ModoSistema.NORMAL) null
                                   else _estadoEmergencia.value.alertaActiva
                )
            }
            .onFailure { Log.e(TAG, "Payload de estado inválido: $payload", it) }
    }

    // --- DTOs ---

    private data class AlertaDto(
        @SerializedName("id")        val id: String,
        @SerializedName("tipo")      val tipo: String,
        @SerializedName("severidad") val severidad: String?,
        @SerializedName("mensaje")   val mensaje: String,
        @SerializedName("timestamp") val timestamp: Long
    ) {
        fun toDominio(edificioId: String) = Alerta(
            id = id,
            tipo = runCatching { TipoAlerta.valueOf(tipo.uppercase()) }.getOrDefault(TipoAlerta.DESCONOCIDA),
            severidad = runCatching { Severidad.valueOf((severidad ?: "").uppercase()) }.getOrDefault(Severidad.DESCONOCIDA),
            mensaje = mensaje,
            edificioId = edificioId,
            timestamp = timestamp
        )
    }

    private data class EstadoDto(
        @SerializedName("modo")      val modo: String,
        @SerializedName("timestamp") val timestamp: Long = 0
    )

    companion object { private const val TAG = "MqttAlertaRepository" }
}
