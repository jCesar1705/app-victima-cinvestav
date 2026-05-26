package mx.cinvestav.emergencias.victima.domain

import kotlinx.coroutines.flow.Flow
import mx.cinvestav.emergencias.victima.domain.model.DatosMedicos
import mx.cinvestav.emergencias.victima.domain.model.ResultadoRegistro

/**
 * Contrato del repositorio de datos médicos (CU-03).
 *
 * Implementaciones:
 *  - [data.fake.FakeSaludRepository]   → simula el nodo FOG, persiste en DataStore local
 *  - [data.remote.SaludRemoteRepository] → REST POST /api/v1/salud al nodo FOG real
 */
interface SaludRepository {

    /** Datos médicos almacenados localmente (caché offline). */
    val datosMedicos: Flow<DatosMedicos?>

    /**
     * Registra o actualiza los datos médicos del usuario.
     * Requiere [DatosMedicos.consentimientoAceptado] == true (R8 — LFPDPPP).
     */
    suspend fun registrarDatos(datos: DatosMedicos): ResultadoRegistro
}
