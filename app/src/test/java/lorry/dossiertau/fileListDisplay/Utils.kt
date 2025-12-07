package lorry.dossiertau.fileListDisplay

import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import lorry.basics.TauInjections
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.diskTransfer.TauRepoFile
import lorry.dossiertau.data.diskTransfer.TauRepoFolder
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.usecases.folderContent.FolderCompo
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.junit.After
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun FileListDisplayTests.prepareKoin(testScheduler: TestCoroutineScheduler) {

    GlobalContext.stopKoin()

    startKoin {
        modules(
            TauInjections,
            module {
                allowOverride(true)
                single<IFolderRepo> { spyk(get<IFolderRepo>(named("real"))) }
                single<IFolderCompo> {
                    spyk(
                        FolderCompo(
                            get(),
                            StandardTestDispatcher(testScheduler)
                        )
                    )
                }
                single<TauViewModel> { spyk(TauViewModel(get())) }
            }
        )
    }
}

fun FileListDisplayTests.FILE_TOTO(parentPath: TauPath) = TauFile(
    parentPath = parentPath,
    name = TauItemName("toto.mp4"),
    modificationDate = TauDate.fromLong(825)
)

fun FileListDisplayTests.REPOFILE_TOTO(parentPath: TauPath) = TauRepoFile(
    parentPath = parentPath,
    name = TauItemName("toto.mp4"),
    modificationDate = TauDate.fromLong(825)
)

fun FileListDisplayTests.FOLDER_DIVERS(parentPath: TauPath) = TauFolder(
    fullPath = TauPath.of("$parentPath/divers"),
    modificationDate = TauDate(834)
)

fun FileListDisplayTests.FOLDER_FULL(path: TauPath) = TauFolder(
    fullPath = path,
    children = listOf(
        FILE_TOTO(path),
        FOLDER_DIVERS(path)
    ),
    modificationDate = TauDate(834)
)

fun FileListDisplayTests.REPOFOLDER_DIVERS(parentPath: TauPath) = TauRepoFolder(
    fullPath = TauPath.of("$parentPath/divers"),
    modificationDate = TauDate(834)
)

@After
fun FileListDisplayTests.tearDownKoin() {
    stopKoin()
}

class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}