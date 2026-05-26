package mx.cinvestav.emergencias.victima.ui.salud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.cinvestav.emergencias.victima.di.AppContainer
import mx.cinvestav.emergencias.victima.domain.model.DatosMedicos
import mx.cinvestav.emergencias.victima.domain.model.ResultadoRegistro
import mx.cinvestav.emergencias.victima.domain.model.TipoSangre
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarDatosMedicosUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.RegistrarDatosMedicosUseCase

data class SaludUiState(
    val tipoSangre: TipoSangre = TipoSangre.DESCONOCIDO,
    val alergias: String = "",
    val condicionesRelevantes: String = "",
    val medicamentos: String = "",
    val consentimientoAceptado: Boolean = false,
    val resultado: ResultadoRegistro = ResultadoRegistro.Idle,
    val datosYaRegistrados: Boolean = false
)

class SaludViewModel(
    private val observarDatos: ObservarDatosMedicosUseCase,
    private val registrarDatos: RegistrarDatosMedicosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaludUiState())
    val uiState: StateFlow<SaludUiState> = _uiState.asStateFlow()

    init {
        // Pre-carga el formulario si el usuario ya registró datos antes.
        viewModelScope.launch {
            observarDatos().collect { datos ->
                if (datos != null) {
                    _uiState.update {
                        it.copy(
                            tipoSangre = datos.tipoSangre,
                            alergias = datos.alergias,
                            condicionesRelevantes = datos.condicionesRelevantes,
                            medicamentos = datos.medicamentos,
                            consentimientoAceptado = datos.consentimientoAceptado,
                            datosYaRegistrados = true
                        )
                    }
                }
            }
        }
    }

    fun onTipoSangreChange(tipo: TipoSangre) =
        _uiState.update { it.copy(tipoSangre = tipo) }

    fun onAlergiasChange(texto: String) =
        _uiState.update { it.copy(alergias = texto) }

    fun onCondicionesChange(texto: String) =
        _uiState.update { it.copy(condicionesRelevantes = texto) }

    fun onMedicamentosChange(texto: String) =
        _uiState.update { it.copy(medicamentos = texto) }

    fun onConsentimientoChange(aceptado: Boolean) =
        _uiState.update { it.copy(consentimientoAceptado = aceptado) }

    fun onGuardar() {
        val estado = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(resultado = ResultadoRegistro.Cargando) }

            val datos = DatosMedicos(
                tipoSangre = estado.tipoSangre,
                alergias = estado.alergias,
                condicionesRelevantes = estado.condicionesRelevantes,
                medicamentos = estado.medicamentos,
                consentimientoAceptado = estado.consentimientoAceptado
            )

            val resultado = registrarDatos(datos)
            _uiState.update { it.copy(resultado = resultado) }

            // Resetea el resultado después de mostrarlo para no re-mostrarlo al rotar.
            if (resultado is ResultadoRegistro.Exito) {
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(resultado = ResultadoRegistro.Idle, datosYaRegistrados = true) }
            }
        }
    }

    fun onDismissError() =
        _uiState.update { it.copy(resultado = ResultadoRegistro.Idle) }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SaludViewModel(
                observarDatos = container.observarDatosMedicos,
                registrarDatos = container.registrarDatosMedicos
            ) as T
    }
}
