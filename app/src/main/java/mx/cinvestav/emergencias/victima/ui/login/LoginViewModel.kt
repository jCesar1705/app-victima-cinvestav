package mx.cinvestav.emergencias.victima.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.cinvestav.emergencias.victima.data.FogPreferences
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LoginUiState(
    val identificador: String = "",
    val password: String      = "",
    val cargando: Boolean     = false,
    val error: String         = "",
    val exitoso: Boolean      = false
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = FogPreferences(app)
    private val http  = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado

    fun actualizarIdentificador(v: String) { _estado.value = _estado.value.copy(identificador = v, error = "") }
    fun actualizarPassword(v: String)      { _estado.value = _estado.value.copy(password = v, error = "") }

    fun iniciarSesion() {
        val id  = _estado.value.identificador.trim()
        val pwd = _estado.value.password

        if (id.isEmpty() || pwd.isEmpty()) {
            _estado.value = _estado.value.copy(error = "Completa todos los campos")
            return
        }
        _estado.value = _estado.value.copy(cargando = true, error = "")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val host    = prefs.fogHost.first()
                val port    = prefs.fogPort.first()
                val baseUrl = "http://$host:$port"

                val json = JSONObject().apply {
                    put("identificador", id)
                    put("password", pwd)
                }.toString()

                val body = json.toRequestBody("application/json".toMediaType())
                val req  = Request.Builder()
                    .url("$baseUrl/api/auth/login")
                    .post(body)
                    .build()

                val resp = http.newCall(req).execute()
                val respBody = resp.body?.string() ?: ""

                if (resp.isSuccessful) {
                    val obj = JSONObject(respBody)
                    prefs.guardarSesion(
                        identificador = obj.getString("identificador"),
                        nombre        = obj.getString("nombre"),
                        rol           = obj.getString("rol")
                    )
                    _estado.value = _estado.value.copy(cargando = false, exitoso = true)
                } else {
                    val msg = runCatching { JSONObject(respBody).getString("error") }
                        .getOrDefault("Credenciales incorrectas")
                    _estado.value = _estado.value.copy(cargando = false, error = msg)
                }
                resp.close()
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error    = "Error de conexion: ${e.message}"
                )
            }
        }
    }
}