package lorry.basics

import org.koin.dsl.module
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.usecases.folderContent.FolderCompo
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.FolderRepo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.koin.core.qualifier.named

val TauInjections = module {
    single<IFolderRepo>(named("real")) { FolderRepo(/*...*/) }
    single<IFolderRepo> { get(named("real")) }          // alias public

    single<IFolderCompo>(named("real")) { FolderCompo(get()) }
    single<IFolderCompo> { get(named("real")) }

    single<TauViewModel>(named("real")) { TauViewModel(get()) }
    single<TauViewModel> { get(named("real")) }

}