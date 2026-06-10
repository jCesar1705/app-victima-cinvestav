package mx.cinvestav.emergencias.victima.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Claves del DataStore de configuracion del nodo FOG y sesion del usuario. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fog_prefs")

object FogPrefsKeys {
    val FOG_HOST          = stringPreferencesKey("fog_host")
    val FOG_PORT          = intPreferencesKey("fog_port")
    val MQTT_PORT         = intPreferencesKey("mqtt_port")
    val USER_IDENTIFICADOR = stringPreferencesKey("user_identificador")
    val USER_NOMBRE       = stringPreferencesKey("user_nombre")
    val USER_ROL          = stringPreferencesKey("user_rol")
    val IS_LOGGED_IN      = booleanPreferencesKey("is_logged_in")
    val EDIFICIO_CLAVE    = stringPreferencesKey("edificio_clave")
}

class FogPreferences(private val context: Context) {

    val fogHost: Flow<String>  = context.dataStore.data.map { it[FogPrefsKeys.FOG_HOST]  ?: "" }
    val fogPort: Flow<Int>     = context.dataStore.data.map { it[FogPrefsKeys.FOG_PORT]  ?: 8080 }
    val mqttPort: Flow<Int>    = context.dataStore.data.map { it[FogPrefsKeys.MQTT_PORT] ?: 1883 }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[FogPrefsKeys.IS_LOGGED_IN] ?: false }
    val userIdentificador: Flow<String> = context.dataStore.data.map { it[FogPrefsKeys.USER_IDENTIFICADOR] ?: "" }
    val userName: Flow<String> = context.dataStore.data.map { it[FogPrefsKeys.USER_NOMBRE] ?: "" }
    val userRol: Flow<String>  = context.dataStore.data.map { it[FogPrefsKeys.USER_ROL]   ?: "USUARIO" }
    val edificioClave: Flow<String> = context.dataStore.data.map { it[FogPrefsKeys.EDIFICIO_CLAVE] ?: "A" }

    suspend fun guardarConexion(host: String, port: Int = 8080, mqttPort: Int = 1883) {
        context.dataStore.edit { prefs ->
            prefs[FogPrefsKeys.FOG_HOST]  = host
            prefs[FogPrefsKeys.FOG_PORT]  = port
            prefs[FogPrefsKeys.MQTT_PORT] = mqttPort
        }
    }

    suspend fun guardarSesion(identificador: String, nombre: String, rol: String) {
        context.dataStore.edit { prefs ->
            prefs[FogPrefsKeys.USER_IDENTIFICADOR] = identificador
            prefs[FogPrefsKeys.USER_NOMBRE]        = nombre
            prefs[FogPrefsKeys.USER_ROL]           = rol
            prefs[FogPrefsKeys.IS_LOGGED_IN]       = true
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { prefs ->
            prefs[FogPrefsKeys.IS_LOGGED_IN]       = false
            prefs[FogPrefsKeys.USER_IDENTIFICADOR] = ""
            prefs[FogPrefsKeys.USER_NOMBRE]        = ""
        }
    }

    /** URL base del nodo FOG, ej. "http://192.168.1.50:8080" */
    suspend fun obtenerBaseUrl(): String {
        var host = ""
        var port = 8080
        context.dataStore.data.collect { prefs ->
            host = prefs[FogPrefsKeys.FOG_HOST] ?: ""
            port = prefs[FogPrefsKeys.FOG_PORT] ?: 8080
        }
        return "http://$host:$port"
    }
}
