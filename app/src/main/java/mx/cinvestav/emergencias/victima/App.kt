package mx.cinvestav.emergencias.victima

import android.app.Application
import mx.cinvestav.emergencias.victima.di.AppContainer

/**
 * Application de la app de víctima/usuario.
 * Crea el contenedor de dependencias y lo expone al resto de la app.
 *
 * Declarar en AndroidManifest.xml:  android:name=".App"
 */
class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * Recrea el AppContainer leyendo la IP y usuario actuales de FogPreferences.
     * Llamar después del login para que el AlertaViewModel conecte con la IP correcta.
     */
    fun reiniciarContainer() {
        container.limpiar()
        container = AppContainer(this)
    }
}