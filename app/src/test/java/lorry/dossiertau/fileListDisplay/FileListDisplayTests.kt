package lorry.dossiertau.fileListDisplay

import android.os.FileObserver
import androidx.room.Ignore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import lorry.dossiertau.data.intelligenceService.CIA
import lorry.dossiertau.data.intelligenceService.utils.CIALevel
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.sameContentAs
import lorry.dossiertau.support.littleClasses.toTauPath
import org.junit.Test
import org.koin.test.KoinTest
import lorry.dossiertau.data.intelligenceService.AirForce
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.model.*
import lorry.dossiertau.support.littleClasses.toTauDate
import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import io.mockk.Runs
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import lorry.dossiertau.data.dbModel.AppDb
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.dbModel.toFileDiffEntity
import lorry.dossiertau.data.intelligenceService.ISpy
import lorry.dossiertau.data.intelligenceService.Spy
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserver
import lorry.dossiertau.data.intelligenceService.utils.TauFileObserverInside
import lorry.dossiertau.data.intelligenceService.utils.events.GlobalSpyLevel
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.usecases.folderContent.support.FolderRepo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(RobolectricTestRunner::class)
class FileListDisplayTests : KoinTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    var db: AppDb? = null
    var dbDao: FileDiffDao? = null

    //////////////
    // test n°1 //
    //////////////
    @Test
    fun `#1 VM setTauFolder() + IFolderCompo ⇒ nvos items`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            //* input
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val compoItems = listOf(
                FILE_TOTO(PATH),
                FOLDER_DIVERS(PATH)
            )
            val repoItems = listOf(
                REPOFILE_TOTO(PATH),
                REPOFOLDER_DIVERS(PATH)
            )

            // premier droppé: celui de la déclaration du MutableStateFlow: [[folderFlowDeclaration]]
            // deuxième droppé: celui de l'init{} du TauViewModel: [[tauViewModelInit]]
            compo.folderFlow.drop(2).test {
                coEvery { repo.getItemsInFullPath(PATH) } returns repoItems

                //act
                vm.setTauFolder(folderPath = PATH)

                //ass
                advanceUntilIdle()
                val newItems = awaitItem()

                //le coVerify est après le awaitItem car ce dernier lance un scope.launch
                //[[coroutine longue]] qui est explicitement attendu pas awaitItem()
                //la vérification se fait après le awaitItem()
                coVerify { repo.getItemsInFullPath(PATH) }
                confirmVerified(repo)

                assert(
                    newItems.fold(
                        ifEmpty = { false },
                        ifSome = { folder -> folder.children.sameContentAs(compoItems) }
                    ))
            }
        }
    }

    //////////////
    // test n°2 //
    //////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService + file created ⇒ diff emitted`() = runTest {

        //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            val cia = CIA()

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()

            spy.updateEventFlow.test {

                advanceUntilIdle()
                cia.spy = spy
                spy.setObservedFolder(PATH)

                //act
                val toto = FILE_TOTO(PATH)
                val fileToEmit = toto.fullPath


                spy.emitFake_CREATEITEM(fileToEmit, ItemType.FILE, 817L.toTauDate())
                //act + arrange
                advanceUntilIdle()
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                expect(decision).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.GlobalRefresh>()
                    feature { f((it as CIALevel.GlobalRefresh)::itemPath) }.toEqual(PATH)
                }

                advanceUntilIdle()
                val event2 = awaitItem()
                val decision2 = cia.manageUpdateEvents(event2)
                //assert
                expect(decision2).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.CreateItem>()
                    feature { f((it as CIALevel.CreateItem)::modificationDate) }.toEqual(
                        817L.toTauDate()
                    )
                }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    //////////////
    // test n°3 //
    //////////////
    // changer de répertoire observé courant (ROC) ⇒ full émis
    // mais NI collecte ni envoi à room
    @Test
    fun `#3 SpyService + folder changed ⇒ global emitted`() = runTest {

        //* SPY ----   events on items   ---->  FBI ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            //arrange
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val cia = CIA()

            /**
             * au démarrage -> EMPTY
             * entrée dans le répertoire: un appel -> returnedFolder
             */
//        every { fakeRepo.readFolder(any()) } returnsMany listOf(
//            //NOTTODO refactor avec value class + sealed class ou cf TauPath
//            TauFolder.EMPTY,
//            returnedFolder
//        )

            spy.updateEventFlow.test {

                /**ce qui déclenche un completeScan:
                 * 1. au démarrage / lors d'un changeFolder
                 *    (cependant cf [[room se mêle du scan de démarrage]])
                 * 2. après N diffs
                 **/
                //act
                spy.setObservedFolder(PATH)
                advanceUntilIdle()

                //assert
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                expect(decision).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.GlobalRefresh>()
                    feature { f(it::itemPath) }.toEqual(PATH)
                }
            }
        }
    }

    //////////////
    // test n°4 //
    //////////////
    @Ignore
    //#[[room se mêle du scan de démarrage]]
    fun `#4 SpyService envoie au démarrage ancien si ∃ dans room && même`() = runTest {

        //* SPY ----   events on items   ---->  FBI ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        prepareKoin(testScheduler)
        val testScope = this

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val cia = CIA()

            cia.scope = testScope
            cia.dispatcher = StandardTestDispatcher(testScheduler)

//        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
//            .allowMainThreadQueries() // OK en test
//            .build()
//        val dao = db.fileDiffDao()
//        val repo = DiffRepository(dao, StandardTestDispatcher(testScheduler))

//        val airForceOne = AirForce(
//            cia = cia,
//            scope = testScope + dispatcher,
//            repo = repo
//        )

//        val airForce = spyk<AirForce>(airForceOne)

            spy.setObservedFolder(PATH)
            //spy.startSurveillance()

            //act
            val toto = FILE_TOTO(PATH)
            val fileToEmit = toto.fullPath

            spy.updateEventFlow.test {

                spy.emitFake_CREATEITEM(fileToEmit, ItemType.FILE, 817L.toTauDate())
                //act + arrange
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                //assert
                expect(decision) {
                    toBeAnInstanceOf<CIALevel.CreateItem>()
                    notToEqualNull()
                    feature({ f(it!!::itemPath) }) { toEqual(toto.fullPath) }
                    feature { f((it!! as CIALevel.CreateItem)::modificationDate) }.toEqual(
                        817L.toTauDate()
                    )
                }
            }
        }
    }

    //////////////
    // test n°5 //
    //////////////
    @Test
    fun `#5 Airforce envoie le diff CREATE_FILE`() = runTest {

        //* SPY ----   events on items   ---->  FBI ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            /**
             * idées à retenir
             * - le même scope pour émission de flux & réception
             * - un flux.each{}.stateIn() doit être un job cancellé à la fin du test
             */

            prepareKoin(testScheduler)
            val testScope = this

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val toto = FILE_TOTO(PATH)

            val cia = CIA()

            cia.scope = testScope + dispatcher
            cia.dispatcher = dispatcher

            val diffRepo: DiffRepository by inject()
            val repo1 = spyk(diffRepo)

            val airForceOne = AirForce(
                scope = testScope + dispatcher,
                repo = repo1
            )

            airForceOne.cia = cia

            val airForce = spyk<AirForce>(airForceOne)
            val job = airForce.startListeningForCIADecisions()

            val createItemDecision = CIALevel.CreateItem(
                eventPath = toto.fullPath,
                modificationDate = toto.modificationDate,
                itemType = ItemType.FILE
            )

            //elle ne fait rien
            coEvery { airForce.modifyDatabaseBy(any()) } just Runs

            //act
            cia.emitCIALevel(createItemDecision)
            advanceUntilIdle()

            //assert
            val dbCommand = DbCommand.CreateItem(toto.toDbFile())
            coVerify { airForce.modifyDatabaseBy(dbCommand) }

            job.cancel()
        }
    }

    //////////////
    // test n°6 //
    //////////////
    @Test
    fun `#6 Airforce + repo transmettent DbCommand⬝CreateItem`() = runTest {

        //* SPY ----   events on items   ---->  FBI ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            /**
             * idées à retenir
             * - le même scope pour émission de flux & réception
             * - un flux.each{}.stateIn() doit être un job cancellé à la fin du test
             */

            prepareKoin(testScheduler)
            val testScope = this

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val toto = FILE_TOTO(PATH)

            val cia = CIA()

            cia.scope = testScope + dispatcher
            cia.dispatcher = dispatcher

            val mockDiffDao = mockk<FileDiffDao>()
            val diffRepo = DiffRepository(mockDiffDao, dispatcher)

            val airForceOne = AirForce(
                scope = testScope + dispatcher,
                repo = diffRepo
            ).apply { this.cia = cia }


            val airForce = spyk<AirForce>(airForceOne)
            val dbCommand = DbCommand.CreateItem(toto.toDbFile())

            coEvery { mockDiffDao.insert(any()) } returns 1L

            //act
            airForce.modifyDatabaseBy(dbCommand)
            advanceUntilIdle()

            //assert
            coVerify(exactly = 1) { mockDiffDao.insert(dbCommand.toFileDiffEntity()) }
        }
    }


    //////////////
    // test n°7 //
    //////////////
    @Test
    fun `#7 DB ajout createFile ⇒ DB a bien un élément`() = runTest {

//        prepareKoin(testScheduler)

        val appDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDb::class.java
        )
            .allowMainThreadQueries() // ok en test
            .build()

//        appDb.invalidationTracker.addObserver(
//            object : InvalidationTracker.Observer("file_diffs") {
//                override fun onInvalidated(tables: Set<String>) {
//                    println("Tables invalidées: $tables")
//                }
//            }
//        )

        //0 éléments
        dbDao = appDb.fileDiffDao()

        val PATH = "/storage/emulated/0/Download".toTauPath()
        val toto = FILE_TOTO(PATH)

        dbDao!!.diffFlow().test {

            val initial = awaitItem()

//                val initial = awaitItem()
//                println("initial = $initial")

            val dbCommand = DbCommand.CreateItem(toto.toDbFile())
            dbDao!!.insert(dbCommand.toFileDiffEntity())
            advanceUntilIdle()

            val entry = awaitItem()  // [diff]
            println("afterInsert = $entry")

//                val dbFile = File("/Users/olivier/Downloads", "tau-db-snapshot.sqlite")
//                val snap = dbFile.absolutePath
//                appDb.openHelper.writableDatabase.execSQL("VACUUM INTO '$snap';")
//                println("Snapshot -> $snap")

//                val kesako = awaitItem()
//                var entry = awaitItem()
            expect(entry).notToEqualNull()
        }
    }

    //////////////
    // test n°8 //
    //////////////
    @Test
    fun `#8 DB ajout createFile ⇒ modif items courants si pertinent`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

//        val appDb: AppDb = Room.databaseBuilder(
//            ApplicationProvider.getApplicationContext(),
//            AppDb::class.java,
//            "tau-db.sqlite"
//        ).build()

            try {
                if (dbDao == null)
                    throw Exception("erreur test #8")

                val INITIALPATH = "/storage/emulated/0/Download".toTauPath()
                val PATH = "/storage/emulated/0/Download".toTauPath()
                val toto = FILE_TOTO(PATH)

                val tenSeconds = 10.toDuration(DurationUnit.SECONDS)

                //Ça force folderPathFlow à devenir Some(PATH) → diffsForFolder(PATH) sera effectivement collecté
                compo.folderFlow.test {

                    // saute tout ce qui ne t'intéresse pas
                    var v = awaitItem()
                    while (v.isNone() || v.getOrNull()?.fullPath?.path != INITIALPATH.path) {
                        v = awaitItem()
                    }

                    expect(v) {
                        its { isSome() }.toEqual(true)
                        its { getOrNull()?.fullPath?.path }.toEqual(INITIALPATH.path)
                        its { getOrNull()?.children?.size }.toEqual(0)
                    }

//                val item1 = awaitItem()
//                println("TEST: item1 = $item1")

                    println("TEST: appel de setTauFolder")
                    vm.setTauFolder(PATH)

                    val fromTauFolder = awaitItem()
                    expect(fromTauFolder) {
                        its { isSome() }.toEqual(true)
                        its { getOrNull()?.fullPath?.path }.toEqual(PATH.path)
                        its { getOrNull()?.children?.size }.toEqual(0)
                    }
                    advanceUntilIdle()

                    val dbCommand = DbCommand.CreateItem(toto.toDbFile())

                    //1. transite dans DB
                    //2. lu et traité par folderCompo
                    dbDao!!.insert(dbCommand.toFileDiffEntity())
                    advanceUntilIdle()

                    val item2 = awaitItem()
                    println("TEST: item3 = $item2")

                    expect(item2) {
                        its { isSome() }.toEqual(true)
                        its { getOrNull()?.children?.size }.toEqual(1)
                    }
                }

            } catch (ex: Exception) {
                //potentiellement le fichier de snapshot existe encore
                println(ex.message)

            } finally {
                db?.close()
//            appDb.close()
            }
        }
    }

    /////////////////
    // test n° 2-2 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - Folder created()`() = runTest {

        //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val cia = CIA()
            cia.spy = spy

            spy.updateEventFlow.test {

                advanceUntilIdle()
                spy.setObservedFolder(PATH)

                val global = awaitItem()
                expect(global) {
                    toBeAnInstanceOf<GlobalSpyLevel>()
                }

                //act
                val divers = FOLDER_DIVERS(PATH)
                val folderToEmit = divers.fullPath

                spy.emitFake_CREATEITEM(folderToEmit, ItemType.FOLDER, 817L.toTauDate())
                //act + arrange
                advanceUntilIdle()
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                //assert
                expect(decision).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.CreateItem>()
                    feature { f((it as CIALevel.CreateItem)::modificationDate) }.toEqual(
                        817L.toTauDate()
                    )
                    feature { f((it as CIALevel.CreateItem)::itemType) }.toEqual(ItemType.FOLDER)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /////////////////
    // test n° 2-3 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - File delete()`() = runTest {

        //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val cia = CIA().apply { this.spy = spy }

            spy.updateEventFlow.test {

                advanceUntilIdle()
                cia.spy = spy

                spy.setObservedFolder(PATH)
                val global = awaitItem()
                expect(global) {
                    toBeAnInstanceOf<GlobalSpyLevel>()
                }

                //act
                val divers = FILE_TOTO(PATH)
                val fileToEmit = divers.fullPath

                spy.emitFake_DELETEITEM(fileToEmit, ItemType.FILE, 817L.toTauDate())
                //act + arrange
                advanceUntilIdle()
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                //assert
                expect(decision).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.DeleteItem>()
                    feature { f((it as CIALevel.DeleteItem)::modificationDate) }.toEqual(
                        817L.toTauDate()
                    )
                    feature { f((it as CIALevel.DeleteItem)::itemType) }.toEqual(ItemType.FILE)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /////////////////
    // test n° 2-4 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - Folder delete()`() = runTest {

        //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val cia = CIA().apply { this.spy = spy }

            spy.updateEventFlow.test {

                advanceUntilIdle()
                cia.spy = spy

                spy.setObservedFolder(PATH)
                val global = awaitItem()
                expect(global) {
                    toBeAnInstanceOf<GlobalSpyLevel>()
                }

                //act
                val divers = FOLDER_DIVERS(PATH)
                val folderToEmit = divers.fullPath

                spy.emitFake_DELETEITEM(folderToEmit, ItemType.FOLDER, 817L.toTauDate())
                //act + arrange
                advanceUntilIdle()
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                //assert
                expect(decision).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.DeleteItem>()
                    feature { f((it as CIALevel.DeleteItem)::modificationDate) }.toEqual(
                        817L.toTauDate()
                    )
                    feature { f((it as CIALevel.DeleteItem)::itemType) }.toEqual(ItemType.FOLDER)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /////////////////
    // test n° 2-5 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - File modify()`() = runTest {

        //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()
            val cia = CIA()
            cia.spy = spy

            spy.updateEventFlow.test {

                advanceUntilIdle()
                cia.spy = spy

                spy.setObservedFolder(PATH)
                val global = awaitItem()
                expect(global) {
                    toBeAnInstanceOf<GlobalSpyLevel>()
                }

                //act
                val toto = FILE_TOTO(PATH)
                val fileToEmit = toto.fullPath

                spy.emitFake_MODIFYITEM(fileToEmit, ItemType.FILE, 817L.toTauDate())
                //act + arrange
                advanceUntilIdle()
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                //assert
                expect(decision).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.ModifyItem>()
                    feature { f((it as CIALevel.ModifyItem)::modificationDate) }.toEqual(
                        817L.toTauDate()
                    )
                    feature { f((it as CIALevel.ModifyItem)::itemType) }.toEqual(ItemType.FILE)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /////////////////
    // test n° 2-5 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - Folder modify()`() = runTest {

        //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        val dispatcher = StandardTestDispatcher(testScheduler)
        TestStuff.configure(dispatcher).use { stuff ->
            val (repo, compo, vm, spy, dbDao) = stuff
            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

            val cia = CIA()
            cia.spy = spy

            //assert
            //* répertoire à observer
            val PATH = "/storage/emulated/0/Download".toTauPath()

            spy.updateEventFlow.test {

                advanceUntilIdle()
                cia.spy = spy

                spy.setObservedFolder(PATH)
                val global = awaitItem()
                expect(global) {
                    toBeAnInstanceOf<GlobalSpyLevel>()
                }

                //act
                val divers = FOLDER_DIVERS(PATH)
                val folderToEmit = divers.fullPath

                spy.emitFake_MODIFYITEM(folderToEmit, ItemType.FOLDER, 817L.toTauDate())
                //act + arrange
                advanceUntilIdle()
                val event = awaitItem()
                val decision = cia.manageUpdateEvents(event)

                //assert
                expect(decision).notToEqualNull() {
                    toBeAnInstanceOf<CIALevel.ModifyItem>()
                    feature { f((it as CIALevel.ModifyItem)::modificationDate) }.toEqual(
                        817L.toTauDate()
                    )
                    feature { f((it as CIALevel.ModifyItem)::itemType) }.toEqual(ItemType.FOLDER)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    /////////////////
    // test n° 2-6 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - File moved_from()`() =
        runTest {

            //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
            //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

            val dispatcher = StandardTestDispatcher(testScheduler)
            TestStuff.configure(dispatcher).use { stuff ->
                val (repo, compo, vm, spy, dbDao) = stuff
                setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

                //assert
                //* répertoire à observer
                val PATH = "/storage/emulated/0/Download".toTauPath()
                val cia = CIA()
                cia.spy = spy

                spy.updateEventFlow.test {

                    advanceUntilIdle()
                    cia.spy = spy

                    spy.setObservedFolder(PATH)
                    val global = awaitItem()
                    expect(global) {
                        toBeAnInstanceOf<GlobalSpyLevel>()
                    }

                    //act
                    val toto = FILE_TOTO(PATH)
                    val fileToEmit = toto.fullPath

                    spy.emitFake_MOVEDFROM(fileToEmit, ItemType.FILE, 817L.toTauDate())
                    //act + arrange
                    advanceUntilIdle()
                    val event = awaitItem()
                    val decision = cia.manageUpdateEvents(event)

                    //assert
                    expect(decision).notToEqualNull() {
                        toBeAnInstanceOf<CIALevel.DeleteItem>()
                        feature { f((it as CIALevel.DeleteItem)::modificationDate) }.toEqual(
                            817L.toTauDate()
                        )
                        feature { f((it as CIALevel.DeleteItem)::itemType) }.toEqual(
                            ItemType.FILE
                        )
                    }

                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    /////////////////
    // test n° 2-7 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - Folder moved_from()`() =
        runTest {

            //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
            //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

            val dispatcher = StandardTestDispatcher(testScheduler)
            TestStuff.configure(dispatcher).use { stuff ->
                val (repo, compo, vm, spy, dbDao) = stuff
                setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

                //assert
                //* répertoire à observer
                val PATH = "/storage/emulated/0/Download".toTauPath()
                val cia = CIA()
                cia.spy = spy

                spy.updateEventFlow.test {

                    advanceUntilIdle()
                    cia.spy = spy

                    spy.setObservedFolder(PATH)
                    val global = awaitItem()
                    expect(global) {
                        toBeAnInstanceOf<GlobalSpyLevel>()
                    }

                    advanceUntilIdle()
                    println("observedFolder = ${cia.spy.observedFolderFlow.value}")

                    //act
                    val divers = FOLDER_DIVERS(PATH)
                    val folderToEmit = divers.fullPath

                    spy.emitFake_MOVEDFROM(folderToEmit, ItemType.FOLDER, 817L.toTauDate())
                    //act + arrange
                    advanceUntilIdle()
                    val event = awaitItem()
                    val decision = cia.manageUpdateEvents(event)

                    //assert
                    expect(decision).notToEqualNull() {
                        toBeAnInstanceOf<CIALevel.DeleteItem>()
                        feature { f((it as CIALevel.DeleteItem)::modificationDate) }.toEqual(
                            817L.toTauDate()
                        )
                        feature { f((it as CIALevel.DeleteItem)::itemType) }.toEqual(
                            ItemType.FOLDER
                        )
                    }

                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    /////////////////
    // test n° 2-8 //
    /////////////////
    // le diff est émis mais pas encore envoyé (c'est le rôle d'AirForce)
    @Test
    fun `#2 SpyService - royaume des changements sur le disque - OTHER Folder created()`() =
        runTest {

            //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
            //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

            val dispatcher = StandardTestDispatcher(testScheduler)
            TestStuff.configure(dispatcher).use { stuff ->
                val (repo, compo, vm, spy, dbDao) = stuff
                setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler)

                //assert
                //* répertoire à observer
                val PATH = "/storage/emulated/0/Download".toTauPath()
                val OTHERPATH = "/storage/emulated/0/Documents".toTauPath()
                val cia = CIA()
                cia.spy = spy

                spy.updateEventFlow.test {

                    advanceUntilIdle()
                    cia.spy = spy

                    spy.setObservedFolder(PATH)
                    val global = awaitItem()
                    expect(global).toBeAnInstanceOf<GlobalSpyLevel>()

                    //act
                    val divers = FOLDER_DIVERS(OTHERPATH)
                    val folderToEmit = divers.fullPath

                    spy.emitFake_CREATEITEM(folderToEmit, ItemType.FOLDER, 817L.toTauDate())
                    //act + arrange
                    advanceUntilIdle()
                    val event = awaitItem()
                    val decision = cia.manageUpdateEvents(event)

                    //assert
                    expect(decision).toEqual(null)
                    expectNoEvents()
                }

                @Before
                fun setUp() {

                }

                @After
                fun tearDownKoin() {
                    stopKoin()
                    db?.close()
                    GlobalContext.getOrNull()?.get<AppDb>()?.close()
                    stopKoin()
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#9 Spy - changement dossier ⇒  demande nouveau snapshot`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo: IFolderRepo = spy<IFolderRepo>(FolderRepo())
        val spy = spy<ISpy>(Spy(
            dispatcher = dispatcher,
            fileObserver = TauFileObserver.of(TauFileObserverInside.DISABLED),
            fileRepo = repo
        ))

        val PATH = "/storage/emulated/0/Download".toTauPath()
        val FAKE_SNAPSHOT = Snapshot.FAKE(PATH)

        //arrange
        everySuspend { repo.createSnapshotFor(PATH) } returns FAKE_SNAPSHOT

        //act
        spy.setObservedFolder(PATH)
        advanceUntilIdle()

        //assert
        verifySuspend { repo.createSnapshotFor(PATH) }
//        coVerify { repo.createSnapshotFor(PATH) }
        expect(spy.newSnapshot).toEqual(FAKE_SNAPSHOT)
    }
}


