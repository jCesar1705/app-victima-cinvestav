package mx.cinvestav.emergencias.victima.domain.usecase

import kotlinx.coroutines.flow.Flow
import mx.cinvestav.emergencias.victima.domain.SaludRepository
import mx.cinvestav.emergencias.victima.domain.model.DatosMedicos
import mx.cinvestav.emergencias.victima.domain.model.ResultadoRegistro

/** CU-03: observar los datos médicos almacenados localmente. */
class ObservarDatosMedicosUseCase(private val repository: SaludRepository) {
    operator fun invoke(): Flow<DatosMedicos?> = repository.datosMedicos
}

/**
 * CU-03: registrar datos médicos.
 * Valida que el consentimiento esté aceptado antes de delegar al repositorio (R8).
 */
class RegistrarDatosMedicosUseCase(private val repository: SaludRepository) {
    suspend operator fun invoke(datos: DatosMedicos): ResultadoRegistro {
        if (!datos.consentimientoAceptado) {
            return ResultadoRegistro.Error(
                "Debes aceptar el aviso de privacidad para registrar tus datos (LFPDPPP)."
            )
        }
        return repository.registrarDatos(datos)
    }
}
