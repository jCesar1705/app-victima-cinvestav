package mx.cinvestav.emergencias.victima.data.remote.mqtt

object MqttTopics {
    fun alertas(edificioId: String)    = "cinvestav/$edificioId/alertas"
    fun estado(edificioId: String)     = "cinvestav/$edificioId/estado"
    fun heartbeats(edificioId: String) = "cinvestav/$edificioId/heartbeats"  // T4
}
