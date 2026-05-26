package mx.cinvestav.emergencias.victima.data.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import mx.cinvestav.emergencias.victima.data.local.SaludDataStore
import mx.cinvestav.emergencias.victima.domain.SaludRepository
import mx.cinvestav.emergencias.victima.domain.model.DatosMedicos
import mx.cinvestav.emergencias.victima.domain.model.ResultadoRegistro

/**
 * Repositorio fake de salud (CU-03).
 *
 * Simula el POST /api/v1/salud al nodo FOG y persiste los datos en DataStore local.
 * El flujo es idéntico al real: validación → "envío" → persistencia local.
 */
class FakeSaludRepository(
    private val dataStore: SaludDataStore
) : SaludRepository {

    override val datosMedicos: Flow<DatosMedicos?> = dataStore.datosMedicos

    override suspend fun registrarDatos(datos: DatosMedicos): ResultadoRegistro {
        return try {
            // Simula la latencia de red del POST al nodo FOG.
            delay(800)

            // Persiste localmente (caché offline).
            val datosConTimestamp = datos.copy(timestamp = System.currentTimeMillis())
            dataStore.guardar(datosConTimestamp)

            ResultadoRegistro.Exito
        } catch (e: Exception) {
            ResultadoRegistro.Error("Error al guardar los datos: ${e.message}")
        }
    }
}
