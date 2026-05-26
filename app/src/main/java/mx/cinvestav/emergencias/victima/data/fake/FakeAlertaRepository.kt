package mx.cinvestav.emergencias.victima.data.fake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.cinvestav.emergencias.victima.domain.AlertaRepository
import mx.cinvestav.emergencias.victima.domain.SimuladorAlertas
import mx.cinvestav.emergencias.victima.domain.model.Alerta
import mx.cinvestav.emergencias.victima.domain.model.EstadoConexion
import mx.cinvestav.emergencias.victima.domain.model.EstadoEmergencia
import mx.cinvestav.emergencias.victima.domain.model.ModoSistema
import mx.cinvestav.emergencias.victima.domain.model.Severidad
import mx.cinvestav.emergencias.victima.domain.model.TipoAlerta
import java.util.UUID

/**
 * Fuente de alertas FALSA. Simula el comportamiento del nodo FOG sin red.
 *
 * Sirve para desarrollar y demostrar el CU-08 mientras el nodo FOG se construye
 * en paralelo. Implementa además [SimuladorAlertas] para que la UI de pruebas
 * pueda disparar una alerta y su fin manualmente.
 *
 * @param scope scope de la aplicación; se inyecta para lanzar la simulación de
 *              "conectando -> conectado".
 * @param edificioId edificio simulado.
 */
class FakeAlertaRepository(
    private val scope: CoroutineScope,
    private val edificioId: String = "edificioA"
) : AlertaRepository, SimuladorAlertas {

    private val _estadoConexion = MutableStateFlow(EstadoConexion.DESCONECTADO)
    override val estadoConexion: StateFlow<EstadoConexion> = _estadoConexion.asStateFlow()

    private val _estadoEmergencia = MutableStateFlow(EstadoEmergencia())
    override val estadoEmergencia: StateFlow<EstadoEmergencia> = _estadoEmergencia.asStateFlow()

    private val _eventosAlerta = MutableSharedFlow<Alerta>(extraBufferCapacity = 8)
    override val eventosAlerta: Flow<Alerta> = _eventosAlerta.asSharedFlow()

    override fun conectar() {
        // Simula la latencia de conexión al broker del nodo FOG.
        scope.launch {
            _estadoConexion.value = EstadoConexion.CONECTANDO
            delay(800)
            _estadoConexion.value = EstadoConexion.CONECTADO
        }
    }

    override fun desconectar() {
        _estadoConexion.value = EstadoConexion.DESCONECTADO
    }

    // --- SimuladorAlertas (controles de prueba) ---

    override fun simularAlertaSismica() {
        val alerta = Alerta(
            id = UUID.randomUUID().toString(),
            tipo = TipoAlerta.SISMICA,
            severidad = Severidad.ALTA,
            mensaje = "Alerta sísmica. Diríjase a la zona segura más cercana.",
            edificioId = edificioId,
            timestamp = System.currentTimeMillis()
        )
        // El nodo FOG real haría exactamente esto: publicar la alerta y el estado.
        _estadoEmergencia.value = EstadoEmergencia(
            modo = ModoSistema.EMERGENCIA,
            alertaActiva = alerta
        )
        _eventosAlerta.tryEmit(alerta)
    }

    override fun simularFinEmergencia() {
        // Bloqueo automático al finalizar la emergencia (QA-10, R7):
        // el modo regresa a NORMAL y se limpia la alerta activa.
        _estadoEmergencia.value = EstadoEmergencia(
            modo = ModoSistema.NORMAL,
            alertaActiva = null
        )
    }
}
