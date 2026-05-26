package mx.cinvestav.emergencias.victima.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mx.cinvestav.emergencias.victima.domain.AlertaRepository
import mx.cinvestav.emergencias.victima.domain.model.Alerta
import mx.cinvestav.emergencias.victima.domain.model.EstadoConexion
import mx.cinvestav.emergencias.victima.domain.model.EstadoEmergencia

/**
 * Casos de uso del subsistema de alertas.
 *
 * Para el prototipo son delgados (delegan al repositorio), pero existen porque
 * son el punto natural donde luego se agregará lógica (p. ej. registrar métricas
 * de QA-03, decidir si vibrar, encolar acuse, etc.) sin tocar la UI.
 */

/** CU-08: observar el estado de emergencia (modo + alerta activa). */
class ObservarEmergenciaUseCase(
    private val repository: AlertaRepository
) {
    operator fun invoke(): StateFlow<EstadoEmergencia> = repository.estadoEmergencia
}

/** CU-08: observar eventos puntuales de alerta (para notificar). */
class ObservarEventosAlertaUseCase(
    private val repository: AlertaRepository
) {
    operator fun invoke(): Flow<Alerta> = repository.eventosAlerta
}

/** Observar el estado de la conexión con el nodo FOG. */
class ObservarConexionUseCase(
    private val repository: AlertaRepository
) {
    operator fun invoke(): StateFlow<EstadoConexion> = repository.estadoConexion
}

/** Gestiona el ciclo de conexión con el nodo FOG. */
class GestionarConexionUseCase(
    private val repository: AlertaRepository
) {
    fun conectar() = repository.conectar()
    fun desconectar() = repository.desconectar()
}
