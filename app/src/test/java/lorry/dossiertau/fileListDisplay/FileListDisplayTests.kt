package lorry.dossiertau.fileListDisplay

import androidx.room.Ignore
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import arrow.core.raise.catch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.intelligenceService.CIA
import lorry.dossiertau.data.intelligenceService.Spy
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.model.fullPath
import lorry.dossiertau.data.model.sameContentAs
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject
import lorry.dossiertau.data.intelligenceService.AirForce
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.model.*
import lorry.dossiertau.support.littleClasses.toTauDate
import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.Runs
import io.mockk.coJustRun
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.plus
import lorry.dossiertau.data.dbModel.AppDb
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.dbModel.FileDiffDao_Impl
import lorry.dossiertau.data.dbModel.toFileDiffEntity
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.support.littleClasses.path
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.stopKoin
import org.robolectric.RobolectricTestRunner
import java.io.File

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

        prepareKoin(testScheduler)

        val fakeRepo: IFolderRepo by inject()
        val fakeCompo: IFolderCompo by inject()
        val fakeVM: TauViewModel by inject()

        //* input
        val PATH = "/storage/emulated/0/Download".toTauPath()
        val compoItems = listOf<TauItem>(
            FILE_TOTO(PATH),
            FOLDER_DIVERS(PATH)
        )
        val repoItems = listOf(
            REPOFILE_TOTO(PATH),
            REPOFOLDER_DIVERS(PATH)
        )

        // premier droppé: celui de la déclaration du MutableStateFlow: [[folderFlowDeclaration]]
        // deuxième droppé: celui de l'init{} du TauViewModel: [[tauViewModelInit]]
        fakeCompo.folderFlow.drop(2).test {
            coEvery { fakeRepo.getItemsInFullPath(PATH) } returns repoItems

            //act
            fakeVM.setTauFolder(folderPath = PATH)

            //ass
            advanceUntilIdle()
            val newItems = awaitItem()

            //le coVerify est après le awaitItem car ce dernier lance un scope.launch
            //[[coroutine longue]] qui est explicitement attendu pas awaitItem()
            //la vérification se fait après le awaitItem()
            coVerify { fakeRepo.getItemsInFullPath(PATH) }
            confirmVerified(fakeRepo)

            assert(
                newItems.fold(
                    ifEmpty = { false },
                    ifSome = { folder -> folder.children.sameContentAs(compoItems) }
                ))
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

        prepareKoin(testScheduler)

        //assert
        //* répertoire à observer
        val PATH = "/storage/emulated/0/Download".toTauPath()

        val spy = Spy(StandardTestDispatcher(testScheduler))
        spy.updateEventFlow.test {

            spy.setObservedFolder(PATH)

            //act
            val toto = FILE_TOTO(PATH)
            val fileToEmit = toto.fullPath


            spy.emitFake_CREATEFILE(fileToEmit, ItemType.FILE, 817L.toTauDate())
            //act + arrange
            advanceUntilIdle()
            val event = awaitItem()
            val decision = CIA.manageUpdateEvents(event)

            //assert
            expect(decision).notToEqualNull() {
                toBeAnInstanceOf<TransferingDecision.CreateFile>()
                feature { f((it as TransferingDecision.CreateFile)::modificationDate) }.toEqual(817L.toTauDate())
            }

            cancelAndIgnoreRemainingEvents()
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

        //arrange
        //* répertoire à observer
        val PATH = "/storage/emulated/0/Download".toTauPath()
        val spy = Spy(StandardTestDispatcher(testScheduler))

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
            val decision = CIA.manageUpdateEvents(event)

            expect(decision).notToEqualNull() {
                toBeAnInstanceOf<TransferingDecision.GlobalRefresh>()
                feature { f(it::filePath) }.toEqual(PATH)
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
        //assert
        //* répertoire à observer
        val PATH = "/storage/emulated/0/Download".toTauPath()

        val spy = Spy(StandardTestDispatcher(testScheduler))
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

            spy.emitFake_CREATEFILE(fileToEmit, ItemType.FILE, 817L.toTauDate())
            //act + arrange
            val event = awaitItem()
            val decision = CIA.manageUpdateEvents(event)

            //assert
            expect(decision) {
                toBeAnInstanceOf<TransferingDecision.CreateFile>()
                notToEqualNull()
                feature({ f(it!!::filePath) }) { toEqual(toto.fullPath) }
                feature { f((it!! as TransferingDecision.CreateFile)::modificationDate) }.toEqual(
                    817L.toTauDate()
                )
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

        /**
         * idées à retenir
         * - le même scope pour émission de flux & réception
         * - un flux.each{}.stateIn() doit être un job cancellé à la fin du test
         */

        prepareKoin(testScheduler)
        val testScope = this
        val dispatcher = StandardTestDispatcher(testScheduler)

        //assert
        //* répertoire à observer
        val PATH = "/storage/emulated/0/Download".toTauPath()
        val toto = FILE_TOTO(PATH)

        val cia = CIA()

        cia.scope = testScope + dispatcher
        cia.dispatcher = dispatcher

        val repo = mockk<DiffRepository>()

        val airForceOne = AirForce(
            scope = testScope + dispatcher,
            repo = repo
        )

        airForceOne.cia = cia

        val airForce = spyk<AirForce>(airForceOne)
        val job = airForce.startListeningForCIADecisions()

        val createFileDecision = TransferingDecision.CreateFile(
            eventFilePath = toto.fullPath,
            modificationDate = toto.modificationDate
        )

        //elle ne fait rien
        coEvery { airForce.modifyDatabaseBy(any()) } just Runs

        //act
        cia.emitCIADecision(createFileDecision)
        advanceUntilIdle()

        //assert
        val dbCommand = DbCommand.CreateItem(toto.toDbFile())
        coVerify { airForce.modifyDatabaseBy(dbCommand) }

        job.cancel()
    }

    //////////////
    // test n°6 //
    //////////////
    @Test
    fun `#6 Airforce + repo transmettent DbCommand⬝CreateItem`() = runTest {

        //* SPY ----   events on items   ---->  FBI ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        /**
         * idées à retenir
         * - le même scope pour émission de flux & réception
         * - un flux.each{}.stateIn() doit être un job cancellé à la fin du test
         */

        prepareKoin(testScheduler)
        val testScope = this
        val dispatcher = StandardTestDispatcher(testScheduler)

        //assert
        //* répertoire à observer
        val PATH = "/storage/emulated/0/Download".toTauPath()
        val toto = FILE_TOTO(PATH)

        val cia = CIA()

        cia.scope = testScope + dispatcher
        cia.dispatcher = dispatcher

        val dao = mockk<FileDiffDao>()
        val repo = DiffRepository(dao, StandardTestDispatcher(testScheduler))

        val airForceOne = AirForce(
            scope = testScope + dispatcher,
            repo = repo
        ).apply { this.cia = cia }


        val airForce = spyk<AirForce>(airForceOne)
        val dbCommand = DbCommand.CreateItem(toto.toDbFile())

        coEvery { dao.insert(any()) } returns 1L

        //act
        airForce.modifyDatabaseBy(dbCommand)
        advanceUntilIdle()

        //assert
        coVerify(exactly = 1) { dao.insert(dbCommand.toFileDiffEntity()) }
    }


    //////////////
    // test n°7 //
    //////////////
    @Test
    fun `#7 DB ajout createFile ⇒ modif items courants si pertinent`() = runTest {

        prepareKoin(testScheduler)
        val appDb: AppDb = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDb::class.java,
            "tau-db.sqlite"
        ).build()

        try {
            dbDao = appDb.fileDiffDao()

            val PATH = "/storage/emulated/0/Download".toTauPath()
            val toto = FILE_TOTO(PATH)

            dbDao!!.diffsForFolder(PATH.path.dropLast(1)).test {

                val dbCommand = DbCommand.CreateItem(toto.toDbFile())
                dbDao!!.insert(dbCommand.toFileDiffEntity())
                advanceUntilIdle()

                val dbFile = File("/Users/olivier/Downloads", "tau-db-snapshot.sqlite")
                if (dbFile.exists())
                    dbFile.delete()
                advanceUntilIdle()

                val snap = dbFile.absolutePath
                appDb.openHelper.writableDatabase.execSQL("VACUUM INTO '$snap';")
                println("Snapshot -> $snap")

                var entry = awaitItem()
                println(entry.size)
            }

        } catch (ex: Exception) {
            println(ex.message)

        }
        finally {
            db?.close()
            appDb.close()
        }
    }

    @Before
    fun setUp() {
//        db = Room.inMemoryDatabaseBuilder(
//            androidContext(),
//            AppDb::class.java
//        )
//            .allowMainThreadQueries()
//            .build()
//
//        dbDao = db?.fileDiffDao()

    }

    @After
    fun tearDownKoin() {
        stopKoin()
        db?.close()
//        val dbFile = File("/Users/olivier/Downloads", "tau-db-snapshot.sqlite")


    }
}


