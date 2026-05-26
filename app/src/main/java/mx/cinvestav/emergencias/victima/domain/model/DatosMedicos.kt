package mx.cinvestav.emergencias.victima.domain.model

/**
 * Datos médicos del usuario (CU-03).
 * Solo se transmiten/descifran durante una emergencia activa (R7, QA-10).
 * El nodo FOG los cifra con AES en reposo — la app los envía en claro sobre TLS.
 */
data class DatosMedicos(
    val tipoSangre: TipoSangre = TipoSangre.DESCONOCIDO,
    val alergias: String = "",
    val condicionesRelevantes: String = "",
    val medicamentos: String = "",
    val consentimientoAceptado: Boolean = false,
    val timestamp: Long = 0L
)

enum class TipoSangre(val etiqueta: String) {
    A_POS("A+"), A_NEG("A-"),
    B_POS("B+"), B_NEG("B-"),
    AB_POS("AB+"), AB_NEG("AB-"),
    O_POS("O+"), O_NEG("O-"),
    DESCONOCIDO("No sé / No especificar")
}

/** Resultado del intento de registro de datos médicos. */
sealed class ResultadoRegistro {
    object Idle : ResultadoRegistro()
    object Cargando : ResultadoRegistro()
    object Exito : ResultadoRegistro()
    data class Error(val mensaje: String) : ResultadoRegistro()
}
