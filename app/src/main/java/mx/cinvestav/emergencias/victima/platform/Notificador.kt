package mx.cinvestav.emergencias.victima.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mx.cinvestav.emergencias.victima.domain.model.Alerta

/**
 * Muestra la alerta sísmica como notificación del sistema (salida visible de CU-08).
 *
 * Usa máxima prioridad para que aparezca como "heads-up" incluso si la app no está
 * en primer plano. Requiere el permiso POST_NOTIFICATIONS (API 33+), que se solicita
 * en MainActivity.
 */
class Notificador(private val context: Context) {

    init {
        crearCanal()
    }

    private fun crearCanal() {
        val canal = NotificationChannel(
            CANAL_ID,
            "Alertas sísmicas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de emergencia del nodo FOG"
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(canal)
    }

    fun notificarAlerta(alerta: Alerta) {
        // Verifica el permiso en runtime para no lanzar SecurityException en API 33+.
        val concedido = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) return

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Alerta sísmica")
            .setContentText(alerta.mensaje)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(alerta.id.hashCode(), notificacion)
    }

    companion object {
        const val CANAL_ID = "canal_alertas"
    }
}
