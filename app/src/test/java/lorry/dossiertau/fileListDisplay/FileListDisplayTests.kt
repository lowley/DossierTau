package lorry.dossiertau.fileListDisplay

import app.cash.turbine.test
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


class FileListDisplayTests : KoinTest {

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
            val decision = CIA.sortUpdateEvents(event)

            //assert
            expect(decision).notToEqualNull(){
                toBeAnInstanceOf<TransferingDecision.CreateFile>()
                feature { f((it as TransferingDecision.CreateFile)::modificationDate)}.toEqual(817L.toTauDate())
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    //////////////
    // test n°3 //
    //////////////
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
            val decision = CIA.sortUpdateEvents(event)

            expect(decision).notToEqualNull() {
                toBeAnInstanceOf<TransferingDecision.GlobalRefresh>()
                feature{ f(it::filePath) }.toEqual(PATH)
            }
        }
    }

    //////////////
    // test n°4 //
    //////////////
    //#[[room se mêle du scan de démarrage]]
    fun `SpyService envoie au démarrage ancien si ∃ dans room && même`() = runTest {

        //* SPY ----   events on items   ---->  FBI ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        prepareKoin(testScheduler)

        val fakeRepo: IFolderRepo by inject()
        val fakeCompo: IFolderCompo by inject()
        val fakeVM: TauViewModel by inject()


        //assert
        //* répertoire à observer
        val PATH = "/storage/emulated/0/Download".toTauPath()

        val spy = Spy(StandardTestDispatcher(testScheduler))
        val airForce = AirForce()
//        val decisionLogic = FBI::makeYourMind(spy.DiskEventFlow)
        spy.setObservedFolder(PATH)
        //spy.startSurveillance()

        //act
        val toto = FILE_TOTO(PATH)
        val fileToEmit = toto.fullPath

        spy.updateEventFlow.test {

            spy.emitFake_CREATEFILE(fileToEmit, ItemType.FILE, 817L.toTauDate())
            //act + arrange
            val event = awaitItem()
            val decision = CIA.sortUpdateEvents(event)

            //assert
            expect(decision){
                toBeAnInstanceOf<TransferingDecision.CreateFile>()
                notToEqualNull()
                feature ({f(it!!::filePath)}){ toEqual(toto.fullPath)}
                feature {f((it!! as TransferingDecision.CreateFile)::modificationDate)}.toEqual(817L.toTauDate())
            }
        }
    }
}

