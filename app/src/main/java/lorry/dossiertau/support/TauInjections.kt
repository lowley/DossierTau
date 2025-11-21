package lorry.basics

import org.koin.dsl.module
import lorry.dossiertau.TauViewModel

val periscopeInjections = module {

    single { TauViewModel() }

}