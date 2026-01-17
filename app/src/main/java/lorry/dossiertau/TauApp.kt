package lorry.dossiertau

import android.app.Application
import lorry.basics.TauInjections
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class TauApp: Application() {

    companion object{
        lateinit var instance: TauApp
            private set
    }

    val app: KoinApplication = koinApplication {
        androidContext(this@TauApp)
        modules(TauInjections)
    }

    val koin: Koin
        get() = app.koin

    init{
        startKoin(app)
        instance = this
    }
}