package lorry.dossiertau.fileListDisplay

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mokkery.spy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import lorry.basics.TauInjections
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.dbModel.AppDb
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.diskTransfer.TauRepoFile
import lorry.dossiertau.data.diskTransfer.TauRepoFolder
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.data.intelligenceService.Spy
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside
import lorry.dossiertau.data.intelligenceService.utils2.events.SnapshotElement
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.intelligenceService.utils2.repo.ISpyRepo
import lorry.dossiertau.data.intelligenceService.utils2.repo.SpyRepo
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.usecases.folderContent.FolderCompo
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.FolderRepo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun FileListDisplayTests.prepareKoin(testScheduler: TestCoroutineScheduler) {

    GlobalContext.stopKoin()

    startKoin {
        modules(
            TauInjections,
            module {
                allowOverride(true)
                single {
                    Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDb::class.java
                    )
                        .allowMainThreadQueries() // ok en test
                        .build()
                }
                single { get<AppDb>().fileDiffDao() }
                single<IFolderRepo> { spy(get<IFolderRepo>(named("real"))) }
                single<IFolderCompo> {
                    FolderCompo(
                        folderRepo = get<IFolderRepo>(),
                        dispatcher = StandardTestDispatcher(testScheduler),
                        fileDiffDAO = get<FileDiffDao>()
                    )
                }
                single<TauViewModel> { TauViewModel(get(), get()) }

                single<CoroutineDispatcher> { Dispatchers.IO }

//                single {
//                    Room.databaseBuilder(
//                        androidContext(),
//                        AppDb::class.java,
//                        "tau-db.sqlite"
//                    )
//                        // .fallbackToDestructiveMigration() // si tu veux démarrer simple
//                        .build()
//                }
//
//                single { get<AppDb>().fileDiffDao() }
                single { DiffRepository(get(), get()) }
            }
        )
    }
}

fun FileListDisplayTests.setAsInjectors(
    repo: IFolderRepo,
    compo: IFolderCompo,
    vm: TauViewModel,
    spy: ISpy,
    dbDao: FileDiffDao,
    testScheduler: TestCoroutineScheduler,
    spyRepo1: ISpyRepo,
) {
    GlobalContext.stopKoin()

    startKoin {
        modules(
            TauInjections,
            module {
                allowOverride(true)

                single<CoroutineDispatcher> { StandardTestDispatcher(testScheduler) }
                single { spy(dbDao) }
                single<IFolderRepo> { spy(repo) }
                single<IFolderCompo> { compo }
                single<TauViewModel> { vm }
                single { spy(spy) }
                single { DiffRepository(get(), get()) }
                single<ISpyRepo> { spy(spyRepo1) }
            }
        )
    }
}

fun FileListDisplayTests.FILE_TOTO(parentPath: TauPath) = TauFile(
    parentPath = parentPath,
    name = TauItemName("toto.mp4"),
    modificationDate = TauDate.fromLong(825),
)

fun FileListDisplayTests.REPOFILE_TOTO(parentPath: TauPath) = TauRepoFile(
    parentPath = parentPath,
    name = TauItemName("toto.mp4"),
    modificationDate = TauDate.fromLong(825)
)

fun FileListDisplayTests.SNAPSHOT_TOTO(parentPath: TauPath) = SnapshotElement(
    name = "TOTO.txt",
    isDir = true,
    size = 523L,
    lastModified = 833L,
    fileId = FileId.fileIdOf(5L, 3L)
)

fun FileListDisplayTests.FOLDER_DIVERS(parentPath: TauPath) = TauFolder(
    fullPath = TauPath.of("${parentPath.path}/divers"),
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

class TestStuff : AutoCloseable {
    lateinit var appDb: AppDb
    lateinit var repo: IFolderRepo
    operator fun component1() = repo
    lateinit var compo: IFolderCompo
    operator fun component2() = compo
    lateinit var vm: TauViewModel
    operator fun component3() = vm
    lateinit var spy: ISpy
    operator fun component4() = spy

    lateinit var dbDao: FileDiffDao
    operator fun component5() = dbDao

    lateinit var spyRepo: ISpyRepo
    operator fun component6() = spyRepo

    override fun close() {
        // 1) couper Koin si tu l’utilises globalement dans les tests
        GlobalContext.stopKoin()
        FolderCompo.collectFillLaunched = false

        // 2) fermer Room
        try {
            appDb.close()
        } catch (_: Throwable) {
        }

        // 3) si tu as des scopes internes dans VM/Compo/Spy, annule-les ici
        // (ex: vm.clear(), compo.stop(), spy.stop()) si tu as ces APIs.
    }

    companion object {
        fun configure(dispatcher: TestDispatcher): TestStuff {

            val result = TestStuff()

            result.appDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDb::class.java
            )
                .allowMainThreadQueries() // ok en test
                .build()

            try {
                result.dbDao = result.appDb.fileDiffDao()

                if (result.dbDao == null)
                    throw Exception("erreur test #8")

                result.spyRepo = spy<ISpyRepo>(SpyRepo())

                result.repo = spy<IFolderRepo>(FolderRepo(result.spyRepo))
                result.compo = FolderCompo(
                    folderRepo = result.repo,
                    dispatcher = dispatcher,
                    fileDiffDAO = result.dbDao!!
                )


                result.spy = spy<ISpy>(
                    Spy(
                        dispatcher = dispatcher,
                        fileObserver = TauFileObserver.of(TauFileObserverInside.DISABLED),
                        fileRepo = result.repo
                    )
                )

                result.vm = TauViewModel(
                    folderCompo = result.compo,
                    spy = result.spy
                )

            } catch (ex: Exception) {
                println(ex.message)
                throw Exception("test #1 init failed")
            }

            return result
        }
    }
}