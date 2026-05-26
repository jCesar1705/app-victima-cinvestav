package mx.cinvestav.emergencias.victima.domain.model

/**
 * Modelos de dominio del subsistema de alertas (CU-08).
 *
 * Esta capa NO depende de Android ni de ninguna librería de red.
 * Son tipos puros que viajan entre la capa de datos y la UI.
 */

/** Tipo de evento de alerta que puede emitir el nodo FOG. */
enum class TipoAlerta {
    SISMICA,    // Alerta sísmica real proveniente del SASMEX / nodo FOG
    SIMULACRO,  // Simulacro de protección civil (R6 — fase de prevención)
    DESCONOCIDA
}

/** Severidad estimada del evento. */
enum class Severidad {
    BAJA, MEDIA, ALTA, DESCONOCIDA
}

/**
 * Alerta sísmica recibida desde el nodo FOG del edificio (CU-08).
 *
 * @param id          identificador único de la alerta (idempotencia / control de duplicados)
 * @param tipo        tipo de evento
 * @param severidad   severidad estimada
 * @param mensaje     texto a mostrar al usuario
 * @param edificioId  edificio (zona de cobertura del nodo FOG) al que pertenece
 * @param timestamp   epoch millis de emisión
 */
data class Alerta(
    val id: String,
    val tipo: TipoAlerta,
    val severidad: Severidad,
    val mensaje: String,
    val edificioId: String,
    val timestamp: Long
)

/**
 * Modo de operación del sistema.
 *
 * Es el eje del "control de acceso contextual" (CU-09 / QA-10 / R7): en NORMAL
 * los datos sensibles (ubicación en tiempo real, datos médicos) están bloqueados;
 * en EMERGENCIA se habilitan. El nodo FOG es la fuente de verdad de este estado y
 * lo propaga a la app. Al concluir la emergencia, el nodo publica NORMAL y la app
 * bloquea automáticamente (sin intervención manual).
 */
enum class ModoSistema {
    NORMAL,
    EMERGENCIA
}

/** Estado de la conexión con el nodo FOG (broker MQTT). */
enum class EstadoConexion {
    DESCONECTADO,
    CONECTANDO,
    CONECTADO
}

/**
 * Estado de emergencia consolidado que observa la UI.
 *
 * @param modo         modo actual del sistema
 * @param alertaActiva alerta que disparó la emergencia (null en modo NORMAL)
 */
data class EstadoEmergencia(
    val modo: ModoSistema = ModoSistema.NORMAL,
    val alertaActiva: Alerta? = null
) {
    val enEmergencia: Boolean get() = modo == ModoSistema.EMERGENCIA
}
