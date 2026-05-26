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
}
