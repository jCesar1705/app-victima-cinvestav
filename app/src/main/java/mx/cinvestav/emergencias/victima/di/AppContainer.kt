package mx.cinvestav.emergencias.victima.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import mx.cinvestav.emergencias.victima.data.fake.FakeAlertaRepository
import mx.cinvestav.emergencias.victima.data.fake.FakeSaludRepository
import mx.cinvestav.emergencias.victima.data.local.SaludDataStore
import mx.cinvestav.emergencias.victima.data.remote.MqttAlertaRepository
import mx.cinvestav.emergencias.victima.data.remote.mqtt.MqttClienteFog
import mx.cinvestav.emergencias.victima.data.FogPreferences
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

    // ── Leer configuración dinámica de FogPreferences ─────────────────
    // (IP configurada por el usuario en IpConfigScreen)
    private val prefs = FogPreferences(appContext)

    private val fogHost: String = runBlocking { prefs.fogHost.first() }
        .takeIf { it.isNotEmpty() } ?: "localhost"

    private val fogPuerto: Int = runBlocking { prefs.mqttPort.first() }

    // Identificador del usuario logueado (reemplaza el hardcodeado del Sprint 1)
    private val victimaId: String = runBlocking { prefs.userIdentificador.first() }
        .takeIf { it.isNotEmpty() } ?: "victima-001"

    // --- Fuente de alertas (CU-08) ---
    val repository: AlertaRepository = if (USE_FAKE) {
        FakeAlertaRepository(appScope, EDIFICIO_ID)
    } else {
        MqttAlertaRepository(
            cliente     = MqttClienteFog(fogHost, fogPuerto, "victima-${UUID.randomUUID()}"),
            edificioId  = EDIFICIO_ID,
            victimaId   = victimaId,
            scope       = appScope,
            heartbeatMs = HEARTBEAT_INTERVALO_MS
        )
    }
    val simulador: SimuladorAlertas? = repository as? SimuladorAlertas

    // --- Fuente de datos médicos (CU-03) ---
    private val saludDataStore = SaludDataStore(appContext)
    val saludRepository: SaludRepository = FakeSaludRepository(saludDataStore)

    // --- Casos de uso CU-08 ---
    val observarEmergencia    = ObservarEmergenciaUseCase(repository)
    val observarEventosAlerta = ObservarEventosAlertaUseCase(repository)
    val observarConexion      = ObservarConexionUseCase(repository)
    val gestionarConexion     = GestionarConexionUseCase(repository)

    // --- Casos de uso CU-03 ---
    val observarDatosMedicos  = ObservarDatosMedicosUseCase(saludRepository)
    val registrarDatosMedicos = RegistrarDatosMedicosUseCase(saludRepository)

    fun limpiar() = appScope.cancel()

    companion object {
        // ⬅️ false para conectar al nodo FOG real
        const val USE_FAKE = false

        // La IP ya no va aquí — viene de FogPreferences (IpConfigScreen)
        const val EDIFICIO_ID            = "edificioA"
        const val HEARTBEAT_INTERVALO_MS = 30_000L
    }
}