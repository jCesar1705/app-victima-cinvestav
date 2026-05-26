package mx.cinvestav.emergencias.victima.data.remote.mqtt

/**
 * CONTRATO DE TÓPICOS MQTT (acuerdo de integración con el nodo FOG).
 *
 * Estos nombres deben coincidir EXACTAMENTE con los que publique el nodo FOG
 * (equipo de Julio) y con los que use la app de brigadista. Si cambian aquí,
 * cambian en los tres componentes.
 *
 * Convención: cinvestav/{edificioId}/{recurso}
 */
object MqttTopics {

    /** Alertas sísmicas que el nodo FOG publica a los dispositivos del edificio (CU-08). */
    fun alertas(edificioId: String) = "cinvestav/$edificioId/alertas"

    /**
     * Estado/modo del sistema para el edificio. El nodo FOG lo publica como
     * mensaje retenido (retained) para que un cliente que se conecta tarde
     * conozca el modo actual de inmediato. Habilita el bloqueo automático (QA-10).
     */
    fun estado(edificioId: String) = "cinvestav/$edificioId/estado"

    // Tópicos de iteraciones siguientes (referencia, aún no usados por la víctima):
    // - cinvestav/{edificioId}/ubicaciones  (CU-02, lo consume la app de brigadista)
    // - cinvestav/{edificioId}/reportes     (CU-04)
}
