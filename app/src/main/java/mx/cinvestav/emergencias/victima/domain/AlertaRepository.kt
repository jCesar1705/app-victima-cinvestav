package mx.cinvestav.emergencias.victima.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mx.cinvestav.emergencias.victima.domain.model.Alerta
import mx.cinvestav.emergencias.victima.domain.model.EstadoConexion
import mx.cinvestav.emergencias.victima.domain.model.EstadoEmergencia

/**
 * Contrato de la fuente de alertas (CU-08).
 *
 * La UI y los casos de uso dependen SOLO de esta interfaz. Tenemos dos
 * implementaciones intercambiables:
 *   - [data.fake.FakeAlertaRepository]  -> simula al nodo FOG (desarrollo sin backend)
 *   - [data.remote.MqttAlertaRepository] -> se conecta al nodo FOG real vía MQTT
 *
 * El intercambio se hace en [di.AppContainer] con una sola bandera. Esto permite
 * desarrollar la app de víctima en paralelo mientras el nodo FOG aún no existe.
 */
interface AlertaRepository {

    /** Estado de la conexión con el nodo FOG. */
    val estadoConexion: StateFlow<EstadoConexion>

    /**
     * Estado de emergencia consolidado (modo + alerta activa).
     * Es la fuente de verdad para habilitar/bloquear funciones sensibles.
     */
    val estadoEmergencia: StateFlow<EstadoEmergencia>

    /**
     * Eventos transitorios de alerta (una emisión por alerta recibida).
     * La UI los usa para disparar la notificación del sistema. A diferencia de
     * [estadoEmergencia], no retiene el último valor para "re-disparar" al re-suscribirse.
     */
    val eventosAlerta: Flow<Alerta>

    /** Inicia la conexión con el nodo FOG y se suscribe a los tópicos de alerta/estado. */
    fun conectar()

    /** Cierra la conexión con el nodo FOG. */
    fun desconectar()
}

/**
 * Controles de simulación, disponibles SOLO cuando se usa la fuente fake.
 * Permiten disparar manualmente una alerta y su fin desde la UI de pruebas,
 * para validar el flujo CU-08 end-to-end sin el nodo FOG real.
 */
interface SimuladorAlertas {
    fun simularAlertaSismica()
    fun simularFinEmergencia()
}
