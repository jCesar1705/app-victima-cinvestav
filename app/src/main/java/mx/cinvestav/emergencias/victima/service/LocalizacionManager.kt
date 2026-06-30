package mx.cinvestav.emergencias.victima.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Maneja el escaneo WiFi y el sensor barométrico para estimar
 * la posición de la víctima durante una emergencia activa (CU-02).
 *
 * Envía el fingerprint al FOG cada vez que se llama [enviarUbicacion].
 * El FOG corre el algoritmo k-NN y devuelve la zona estimada.
 */
class LocalizacionManager(private val context: Context) : SensorEventListener {

    companion object {
        const val TAG = "LocalizacionManager"
    }

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Presión al arrancar el servicio — referencia para calcular el piso. */
    private var presionReferencia: Float? = null
    private var presionActual: Float? = null

    // ── Barometro ─────────────────────────────────────────────────────

    fun iniciarBarometro() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.i(TAG, "Sensor barométrico registrado")
        } else {
            Log.w(TAG, "El dispositivo no tiene sensor barométrico — piso se reportará como 0")
        }
    }

    fun detenerBarometro() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            val presion = event.values[0]
            if (presionReferencia == null) {
                presionReferencia = presion   // primera lectura = planta baja
                Log.i(TAG, "Presión de referencia (PB): $presion hPa")
            }
            presionActual = presion
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    /**
     * Estima el piso actual a partir de la diferencia de presión.
     * Cada ~3m de altura ≈ 0.35 hPa de diferencia.
     * Un piso estándar ≈ 3m → ~0.35 hPa por piso.
     *
     * Positivo = sobre la planta baja, negativo = sotano.
     */
    fun estimarPiso(): Int {
        val ref = presionReferencia ?: return 0
        val actual = presionActual ?: return 0
        val difPresion = ref - actual          // positivo = subiendo
        val alturaMts = difPresion * 8.4f      // ~8.4m por hPa (aprox. atm estándar)
        return Math.round(alturaMts / 3.0f)    // 3m por piso
    }

    // ── Escáner WiFi ──────────────────────────────────────────────────

    /**
     * Lee los resultados del último escaneo WiFi del sistema.
     * Android 9+ limita los escaneos activos a 4 cada 2 minutos,
     * por eso usamos los resultados cacheados — el sistema los actualiza
     * periódicamente de todas formas.
     *
     * Devuelve lista de {bssid, rssi} de todos los APs visibles.
     */
    fun escanearWifi(): List<Pair<String, Int>> {
        return try {
            @Suppress("DEPRECATION")
            wifiManager.scanResults.map { result ->
                Pair(result.BSSID, result.level)   // BSSID = MAC del AP, level = RSSI en dBm
            }.also {
                Log.d(TAG, "APs visibles: ${it.size}")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permiso ACCESS_FINE_LOCATION no otorgado — sin datos WiFi")
            emptyList()
        }
    }

    // ── Envío al FOG ──────────────────────────────────────────────────

    /**
     * Construye el fingerprint WiFi + piso y lo envía al FOG.
     * Solo se llama durante emergencia activa (el FogForegroundService lo controla).
     *
     * POST /api/ubicacion
     * Body: {victimaId, piso, fingerprint:[{bssid,rssi},...]}
     */
    suspend fun enviarUbicacion(
        victimaId: String,
        baseUrl  : String
    ) = withContext(Dispatchers.IO) {
        val aps  = escanearWifi()
        val piso = estimarPiso()

        if (aps.isEmpty()) {
            Log.w(TAG, "Sin APs WiFi visibles — no se puede enviar fingerprint")
            return@withContext
        }

        try {
            val fingerprintArray = JSONArray().apply {
                aps.forEach { (bssid, rssi) ->
                    put(JSONObject().apply {
                        put("bssid", bssid)
                        put("rssi", rssi)
                    })
                }
            }

            val body = JSONObject().apply {
                put("victimaId",  victimaId)
                put("piso",       piso)
                put("fingerprint", fingerprintArray)
            }.toString()

            val url = URL("$baseUrl/api/ubicacion")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod     = "POST"
                doOutput          = true
                connectTimeout    = 5000
                readTimeout       = 5000
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            Log.i(TAG, "Ubicación enviada — APs:${aps.size} piso:$piso HTTP:$code")
            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Error enviando ubicación: ${e.message}")
        }
    }
}