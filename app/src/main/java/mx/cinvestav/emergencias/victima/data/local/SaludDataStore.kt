package mx.cinvestav.emergencias.victima.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.cinvestav.emergencias.victima.domain.model.DatosMedicos

/** DataStore de la app — un único archivo de preferencias. */
private val Context.dataStore by preferencesDataStore(name = "victima_prefs")

/**
 * Persiste los datos médicos del usuario localmente.
 *
 * Usa DataStore + Gson para serializar [DatosMedicos] como JSON.
 * Esto permite recuperarlos en modo offline (QA-14, QA-15) y pre-cargar el formulario
 * si el usuario ya los registró antes.
 *
 * NOTA: En producción estos datos NO se almacenan en claro aquí — el nodo FOG es quien
 * los cifra con AES en reposo. El DataStore local es solo caché de visualización.
 */
class SaludDataStore(context: Context) {

    private val dataStore = context.dataStore
    private val gson = Gson()

    private val DATOS_MEDICOS_KEY = stringPreferencesKey("datos_medicos")

    val datosMedicos: Flow<DatosMedicos?> = dataStore.data.map { prefs ->
        prefs[DATOS_MEDICOS_KEY]?.let { json ->
            runCatching { gson.fromJson(json, DatosMedicos::class.java) }.getOrNull()
        }
    }

    suspend fun guardar(datos: DatosMedicos) {
        dataStore.edit { prefs ->
            prefs[DATOS_MEDICOS_KEY] = gson.toJson(datos)
        }
    }
}
