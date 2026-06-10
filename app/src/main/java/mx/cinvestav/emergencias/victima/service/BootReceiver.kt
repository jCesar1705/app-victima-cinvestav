package mx.cinvestav.emergencias.victima.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.cinvestav.emergencias.victima.data.FogPreferences

/**
 * Al encender el dispositivo, si el usuario ya tenia sesion iniciada,
 * arranca el servicio en segundo plano automaticamente.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val estaLogueado = runBlocking {
            FogPreferences(context).isLoggedIn.first()
        }
        if (estaLogueado) {
            context.startForegroundService(
                Intent(context, FogForegroundService::class.java)
            )
        }
    }
}
