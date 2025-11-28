package lorry.dossiertau

import android.app.Application
import lorry.basics.TauInjections
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication

class TauApp: Application() {

    val app: KoinApplication = koinApplication { modules(TauInjections) }
    val koin: Koin
        get() = app.koin

    init{
        startKoin(app)
    }
}