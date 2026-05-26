package mx.cinvestav.emergencias.victima.data.remote.mqtt

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.cinvestav.emergencias.victima.domain.model.EstadoConexion

/**
 * Envoltura del cliente MQTT (HiveMQ MQTT Client, API asíncrona MQTT 5).
 *
 * Se eligió HiveMQ y NO el clásico Eclipse Paho Android Service porque este
 * último está prácticamente sin mantenimiento y presenta fallos en Android 14+.
 *
 * Para el prototipo se conecta sin TLS (broker de pruebas). En producción
 * (QA-11) debe activarse TLS hacia el broker del nodo FOG.
 *
 * @param host      host del broker (nodo FOG o broker de pruebas)
 * @param port      puerto MQTT (1883 sin TLS)
 * @param clientId  identificador único del cliente
 */
class MqttClienteFog(
    private val host: String,
    private val port: Int,
    private val clientId: String
) {
    private val _estadoConexion = MutableStateFlow(EstadoConexion.DESCONECTADO)
    val estadoConexion: StateFlow<EstadoConexion> = _estadoConexion.asStateFlow()

    private val client: Mqtt5AsyncClient = MqttClient.builder()
        .useMqttVersion5()
        .identifier(clientId)
        .serverHost(host)
        .serverPort(port)
        .automaticReconnectWithDefaultConfig() // robustez: reconexión automática
        .addConnectedListener { _estadoConexion.value = EstadoConexion.CONECTADO }
        .addDisconnectedListener { _estadoConexion.value = EstadoConexion.DESCONECTADO }
        .buildAsync()

    /** Conecta al broker. [onConectado] se invoca al establecer la sesión. */
    fun conectar(onConectado: () -> Unit = {}) {
        _estadoConexion.value = EstadoConexion.CONECTANDO
        client.connectWith()
            .cleanStart(true)
            .keepAlive(30)
            .send()
            .whenComplete { _, error ->
                if (error != null) {
                    Log.e(TAG, "Error al conectar al nodo FOG", error)
                    _estadoConexion.value = EstadoConexion.DESCONECTADO
                } else {
                    _estadoConexion.value = EstadoConexion.CONECTADO
                    onConectado()
                }
            }
    }

    /**
     * Se suscribe a un tópico. El callback se invoca en un hilo de HiveMQ;
     * mantén el procesamiento ligero (parseo + emisión a un Flow).
     */
    fun suscribir(topic: String, onMensaje: (topic: String, payload: String) -> Unit) {
        client.subscribeWith()
            .topicFilter(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback { publish ->
                val payload = String(publish.payloadAsBytes, Charsets.UTF_8)
                onMensaje(publish.topic.toString(), payload)
            }
            .send()
            .whenComplete { _, error ->
                if (error != null) Log.e(TAG, "Error al suscribir a $topic", error)
                else Log.i(TAG, "Suscrito a $topic")
            }
    }

    /** Publica un mensaje (lo usarán iteraciones futuras: ubicación, reportes). */
    fun publicar(topic: String, payload: String) {
        client.publishWith()
            .topic(topic)
            .payload(payload.toByteArray(Charsets.UTF_8))
            .qos(MqttQos.AT_LEAST_ONCE)
            .send()
            .whenComplete { _, error ->
                if (error != null) Log.e(TAG, "Error al publicar en $topic", error)
            }
    }

    fun desconectar() {
        client.disconnect()
        _estadoConexion.value = EstadoConexion.DESCONECTADO
    }

    companion object {
        private const val TAG = "MqttClienteFog"
    }
}
