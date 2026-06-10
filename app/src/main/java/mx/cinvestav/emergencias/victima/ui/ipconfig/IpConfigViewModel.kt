package mx.cinvestav.emergencias.victima.ui.ipconfig

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.cinvestav.emergencias.victima.data.FogPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class IpConfigUiState(
    val host: String       = "",
    val port: String       = "8080",
    val cargando: Boolean  = false,
    val error: String      = "",
    val exito: Boolean     = false
)

class IpConfigViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = FogPreferences(app)
    private val http  = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val _estado = MutableStateFlow(IpConfigUiState())
    val estado: StateFlow<IpConfigUiState> = _estado

    fun actualizarHost(h: String) { _estado.value = _estado.value.copy(host = h, error = "") }
    fun actualizarPort(p: String) { _estado.value = _estado.value.copy(port = p, error = "") }

    /** Prueba la conexion al nodo FOG y si responde guarda la config. */
    fun conectar() {
        val host = _estado.value.host.trim()
        val port = _estado.value.port.trim().toIntOrNull() ?: 8080

        if (host.isEmpty()) {
            _estado.value = _estado.value.copy(error = "Escribe la IP del nodo FOG")
            return
        }

        _estado.value = _estado.value.copy(cargando = true, error = "")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "http://$host:$port/config/status"
                val req = Request.Builder().url(url).build()
                val resp = http.newCall(req).execute()

                if (resp.isSuccessful || resp.code == 404 || resp.code == 403) {
                    // Cualquier respuesta HTTP significa que el servidor está vivo
                    prefs.guardarConexion(host, port)
                    _estado.value = _estado.value.copy(cargando = false, exito = true)
                } else {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = "El nodo respondio con error ${resp.code}"
                    )
                }
                resp.close()
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = "No se pudo conectar a $host:$port. Verifica la IP y que el nodo este corriendo."
                )
            }
        }
    }
}