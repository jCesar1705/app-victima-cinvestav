package mx.cinvestav.emergencias.victima.ui.alerta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.cinvestav.emergencias.victima.di.AppContainer
import mx.cinvestav.emergencias.victima.domain.SimuladorAlertas
import mx.cinvestav.emergencias.victima.domain.model.Alerta
import mx.cinvestav.emergencias.victima.domain.model.EstadoConexion
import mx.cinvestav.emergencias.victima.domain.model.ModoSistema
import mx.cinvestav.emergencias.victima.domain.usecase.GestionarConexionUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarConexionUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarEmergenciaUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarEventosAlertaUseCase
import mx.cinvestav.emergencias.victima.platform.Notificador

/** Estado inmutable que consume la pantalla. */
data class AlertaUiState(
    val conexion: EstadoConexion = EstadoConexion.DESCONECTADO,
    val modo: ModoSistema = ModoSistema.NORMAL,
    val alertaActiva: Alerta? = null,
    val modoPruebaDisponible: Boolean = false
)

class AlertaViewModel(
    private val observarEmergencia: ObservarEmergenciaUseCase,
    private val observarConexion: ObservarConexionUseCase,
    private val observarEventosAlerta: ObservarEventosAlertaUseCase,
    private val gestionarConexion: GestionarConexionUseCase,
    private val notificador: Notificador,
    private val simulador: SimuladorAlertas?
) : ViewModel() {

    val uiState: StateFlow<AlertaUiState> =
        combine(observarConexion(), observarEmergencia()) { conexion, emergencia ->
            AlertaUiState(
                conexion = conexion,
                modo = emergencia.modo,
                alertaActiva = emergencia.alertaActiva,
                modoPruebaDisponible = simulador != null
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlertaUiState(modoPruebaDisponible = simulador != null)
        )

    init {
        // Cada alerta recibida dispara la notificación del sistema (CU-08).
        viewModelScope.launch {
            observarEventosAlerta().collect { alerta ->
                notificador.notificarAlerta(alerta)
            }
        }
        // Conectarse al nodo FOG al iniciar.
        gestionarConexion.conectar()
    }

    // Controles de prueba (solo activos con la fuente fake).
    fun onSimularAlerta() = simulador?.simularAlertaSismica()
    fun onSimularFin() = simulador?.simularFinEmergencia()

    /** Factory que toma las dependencias del AppContainer. */
    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlertaViewModel(
                observarEmergencia = container.observarEmergencia,
                observarConexion = container.observarConexion,
                observarEventosAlerta = container.observarEventosAlerta,
                gestionarConexion = container.gestionarConexion,
                notificador = container.notificador,
                simulador = container.simulador
            ) as T
        }
    }
}
