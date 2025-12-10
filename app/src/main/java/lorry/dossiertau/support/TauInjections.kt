package lorry.basics

import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.dbModel.AppDb
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.intelligenceService.AirForce
import lorry.dossiertau.data.intelligenceService.CIA
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.data.intelligenceService.Spy
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.usecases.folderContent.FolderCompo
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.FolderRepo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.koin.core.qualifier.named

val TauInjections = module {

    single { TauFileObserver.INACTIVE }

    single {
        // Contexte d'application ONLY (pas d’Activity)
        Room.databaseBuilder(get(), AppDb::class.java, "foldertau.db")
            // Si tu utilises sqlite-bundled en prod :
            // .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration() // à remplacer par vrai plan de migration asap
            .build()
    }
    single<FileDiffDao> { get<AppDb>().fileDiffDao() }
    single { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    single { DiffRepository(get()) }

    single { AirForce(get(), get()) }
    single<ISpy> { Spy() }
    single { CIA() }

    single<IFolderRepo>(named("real")) { FolderRepo(/*...*/) }
    single<IFolderRepo> { get(named("real")) }          // alias public

    single<IFolderCompo>(named("real")) { FolderCompo(
        folderRepo = get(),
        fileDiffDAO = get()
    )}

    single<IFolderCompo> { get(named("real")) }



    single<TauViewModel>(named("real")) { TauViewModel(get(), get()) }
    single<TauViewModel> { get(named("real")) }


}

val RoomModule = module {

}