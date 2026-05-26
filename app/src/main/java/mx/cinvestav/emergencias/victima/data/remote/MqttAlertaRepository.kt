package mx.cinvestav.emergencias.victima.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Implementación REAL del repositorio de alertas (CU-08) sobre MQTT.
 *
 * Se suscribe a:
 *   - cinvestav/{edificioId}/alertas -> dispara EMERGENCIA + evento de notificación
 *   - cinvestav/{edificioId}/estado  -> modo autoritativo (habilita bloqueo automático, QA-10)
 *
 * Aún no está conectada a un nodo FOG real; el contrato JSON está fijado abajo
 * (ver DTOs) para que coincida con lo que publique el equipo del nodo.
 */
class MqttAlertaRepository(
    private val cliente: MqttClienteFog,
    private val edificioId: String,
    private val gson: Gson = Gson()
) : AlertaRepository {

    override val estadoConexion: StateFlow<EstadoConexion> = cliente.estadoConexion

    private val _estadoEmergencia = MutableStateFlow(EstadoEmergencia())
    override val estadoEmergencia: StateFlow<EstadoEmergencia> = _estadoEmergencia.asStateFlow()

    private val _eventosAlerta = MutableSharedFlow<Alerta>(extraBufferCapacity = 8)
    override val eventosAlerta: Flow<Alerta> = _eventosAlerta.asSharedFlow()

    override fun conectar() {
        cliente.conectar {
            // Al conectar, suscribirse a los tópicos del edificio.
            cliente.suscribir(MqttTopics.alertas(edificioId)) { _, payload ->
                manejarAlerta(payload)
            }
            cliente.suscribir(MqttTopics.estado(edificioId)) { _, payload ->
                manejarEstado(payload)
            }
        }
    }

    override fun desconectar() = cliente.desconectar()

    private fun manejarAlerta(payload: String) {
        runCatching { gson.fromJson(payload, AlertaDto::class.java) }
            .onSuccess { dto ->
                val alerta = dto.toDominio(edificioId)
                _estadoEmergencia.value = EstadoEmergencia(
                    modo = ModoSistema.EMERGENCIA,
                    alertaActiva = alerta
                )
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
                    // Bloqueo automático al finalizar la emergencia (QA-10, R7).
                    alertaActiva = if (modo == ModoSistema.NORMAL) null
                    else _estadoEmergencia.value.alertaActiva
                )
            }
            .onFailure { Log.e(TAG, "Payload de estado inválido: $payload", it) }
    }

    // --- DTOs: contrato JSON con el nodo FOG ---

    /** JSON esperado en el tópico .../alertas */
    private data class AlertaDto(
        @SerializedName("id") val id: String,
        @SerializedName("tipo") val tipo: String,        // "SISMICA" | "SIMULACRO"
        @SerializedName("severidad") val severidad: String?, // "BAJA" | "MEDIA" | "ALTA"
        @SerializedName("mensaje") val mensaje: String,
        @SerializedName("timestamp") val timestamp: Long
    ) {
        fun toDominio(edificioId: String) = Alerta(
            id = id,
            tipo = runCatching { TipoAlerta.valueOf(tipo.uppercase()) }
                .getOrDefault(TipoAlerta.DESCONOCIDA),
            severidad = runCatching { Severidad.valueOf((severidad ?: "").uppercase()) }
                .getOrDefault(Severidad.DESCONOCIDA),
            mensaje = mensaje,
            edificioId = edificioId,
            timestamp = timestamp
        )
    }

    /** JSON esperado en el tópico .../estado */
    private data class EstadoDto(
        @SerializedName("modo") val modo: String,         // "NORMAL" | "EMERGENCIA"
        @SerializedName("timestamp") val timestamp: Long = 0
    )

    companion object {
        private const val TAG = "MqttAlertaRepository"
    }
}
