package lorry.basics

import androidx.room.Room
import data.ftp.FtpDS
import data.ftp.IFtpDS
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
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside.INACTIVE
import lorry.dossiertau.data.intelligenceService.utils2.repo.SpyRepo
import lorry.dossiertau.ui.bottomSheet.BSVM
import lorry.dossiertau.ui.bottomSheet.support.Browser
import lorry.dossiertau.ui.bottomSheet.support.BrowserViewModel
import lorry.dossiertau.ui.bottomSheet.support.IBrowser
import lorry.dossiertau.ui.breadcrumb.BreadcrumbComponent
import lorry.dossiertau.ui.displayedItem.support.DisplayItemRepo
import lorry.dossiertau.usecases.folderContent.FolderCompo
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.FolderRepo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import lorry.dossiertau.usecases.generateHTMLs.Links
import lorry.dossiertau.usecases.generateHTMLs.VmLinks
import lorry.dossiertau.usecases.generateHTMLs.repos.DiskRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.IDiskRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.INasRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.IWebScrappingRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.NasRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.WebScrappingRepo
import lorry.dossiertau.ui.support.base64.Base64DataSource
import lorry.dossiertau.ui.support.base64.IBase64DataSource
import lorry.dossiertau.ui.support.capsule.CapsuleComponent
import lorry.dossiertau.ui.support.capsule.ICapsuleComponent
import lorry.dossiertau.ui.support.capsule.utilities.FileCapsuleIO
import lorry.folder.items.dossiersigma.external.playing.IPlayingDataSource
import lorry.folder.items.dossiersigma.external.playing.PlayingDataSource
import org.koin.core.qualifier.named

val TauInjections = module {

    single { TauFileObserverInside.INACTIVE }

    single {
        // Contexte d'application ONLY (pas d’Activity)
        Room.databaseBuilder(get(), AppDb::class.java, "foldertau.db")
            // Si tu utilises sqlite-bundled en prod :
            // .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration() // à remplacer par vrai plan de migration asap
//            .setQueryCallback(
//                { sql, bindArgs ->
//                    println("SQL: sql=$sql | args=$bindArgs")
//                },
//                Executors.newSingleThreadExecutor()
//            )
            .build()
    }
    single<FileDiffDao> { get<AppDb>().fileDiffDao() }
    single { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    single { Dispatchers.IO }
    single { DiffRepository(get()) }
    single { SpyRepo() }

    single<BreadcrumbComponent> { BreadcrumbComponent() }

    single { FileCapsuleIO() }
    single<ICapsuleComponent> { CapsuleComponent() }
    single<IBase64DataSource> { Base64DataSource() }

    single<IFolderRepo>(named("real")) { FolderRepo(get<SpyRepo>()) }
    single<IFolderRepo> { get(named("real")) }          // alias public

    single { AirForce(get(), get()) }
    single<ISpy> { Spy(get(), TauFileObserver.of(INACTIVE), get(), get()) }
    single { CIA() }

    single { DisplayItemRepo() }
    single<IPlayingDataSource> { PlayingDataSource(get()) }

    single<IFolderCompo>(named("real")) {
        FolderCompo(
            folderRepo = get(),
            fileDiffDAO = get()
        )
    }

    single { BrowserViewModel() }
    single<IBrowser> { Browser() }
    single<IFolderCompo> { get(named("real")) }

    single { BSVM() }
    single { VmLinks() }
    single<IFtpDS> { FtpDS() }
    single<INasRepo> { NasRepo(FtpDS()) }
    single<IDiskRepo> { DiskRepo() }
    single<IWebScrappingRepo> { WebScrappingRepo() }
    single { Links(get(), get(), get(), get(), get()) }

    single<TauViewModel>(named("real")) { TauViewModel(get(), get(), get(), get()) }
    single<TauViewModel> { get(named("real")) }


}