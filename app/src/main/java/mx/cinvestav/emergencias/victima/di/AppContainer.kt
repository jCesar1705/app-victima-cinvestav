package mx.cinvestav.emergencias.victima.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import mx.cinvestav.emergencias.victima.data.fake.FakeAlertaRepository
import mx.cinvestav.emergencias.victima.data.fake.FakeSaludRepository
import mx.cinvestav.emergencias.victima.data.local.SaludDataStore
import mx.cinvestav.emergencias.victima.data.remote.MqttAlertaRepository
import mx.cinvestav.emergencias.victima.data.remote.mqtt.MqttClienteFog
import mx.cinvestav.emergencias.victima.domain.AlertaRepository
import mx.cinvestav.emergencias.victima.domain.SimuladorAlertas
import mx.cinvestav.emergencias.victima.domain.SaludRepository
import mx.cinvestav.emergencias.victima.domain.usecase.GestionarConexionUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarConexionUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarDatosMedicosUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarEmergenciaUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.ObservarEventosAlertaUseCase
import mx.cinvestav.emergencias.victima.domain.usecase.RegistrarDatosMedicosUseCase
import mx.cinvestav.emergencias.victima.platform.Notificador
import java.util.UUID

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob())
    val notificador = Notificador(appContext)

    // --- Fuente de alertas (CU-08) ---
    val repository: AlertaRepository = if (USE_FAKE) {
        FakeAlertaRepository(appScope, EDIFICIO_ID)
    } else {
        val clienteId = "victima-${UUID.randomUUID()}"
        MqttAlertaRepository(
            cliente = MqttClienteFog(FOG_HOST, FOG_PUERTO, clienteId),
            edificioId = EDIFICIO_ID
        )
    }
    val simulador: SimuladorAlertas? = repository as? SimuladorAlertas

    // --- Fuente de datos médicos (CU-03) ---
    private val saludDataStore = SaludDataStore(appContext)
    val saludRepository: SaludRepository = FakeSaludRepository(saludDataStore)
    // Cuando el nodo FOG esté listo: SaludRemoteRepository(apiService, saludDataStore)

    // --- Casos de uso CU-08 ---
    val observarEmergencia = ObservarEmergenciaUseCase(repository)
    val observarEventosAlerta = ObservarEventosAlertaUseCase(repository)
    val observarConexion = ObservarConexionUseCase(repository)
    val gestionarConexion = GestionarConexionUseCase(repository)

    // --- Casos de uso CU-03 ---
    val observarDatosMedicos = ObservarDatosMedicosUseCase(saludRepository)
    val registrarDatosMedicos = RegistrarDatosMedicosUseCase(saludRepository)

    fun limpiar() = appScope.cancel()

    companion object {
        const val USE_FAKE = true
        const val FOG_HOST = "broker.hivemq.com"
        const val FOG_PUERTO = 1883
        const val EDIFICIO_ID = "edificioA"
    }
}
