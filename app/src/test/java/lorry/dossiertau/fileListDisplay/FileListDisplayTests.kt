package lorry.dossiertau.fileListDisplay

import androidx.compose.ui.Modifier.Companion.any
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
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
import lorry.dossiertau.data.intelligenceService.utils.ItemType
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.support.littleClasses.toTauDate
import net.bytebuddy.matcher.ElementMatchers.any


class FileListDisplayTests : KoinTest {

    @Test
    fun `IFolderCompo récupère le contenu d'un répertoire`() = runTest {

        prepareKoin(testScheduler)

        val fakeRepo: IFolderRepo by inject()
        val fakeCompo: IFolderCompo by inject()
        val fakeVM: TauViewModel by inject()

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
        fakeCompo.folderFlow.drop(2).test {
            coEvery { fakeRepo.getItemsInFullPath(PATH) } returns repoItems

            //act
            fakeVM.setTauFolder(folderPath = PATH)

            //ass
            val newItems = awaitItem()

            //le coVerify est après le awaitItem car ce dernier lance un scope.launch
            //[[coroutine longue]] qui est explicitement attendu pas awaitItem()
            //la vérification se fait après le awaitItem()
            coVerify { fakeRepo.getItemsInFullPath(PATH) }

            assert(
                newItems.fold(
                    ifEmpty = { false },
                    ifSome = { folder -> folder.children.sameContentAs(compoItems) }
                ))
        }
    }

    @Test
    fun `SpyService observe les changements d'un dossier`() = runTest {

        //* SPY ----   events on items   ---->  CIA ---- treated infos     ----> AIRFORCE
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

        spy.incomingEventFlow.test {

            spy.emitFake_CREATEFILE(fileToEmit, ItemType.FILE, 817L.toTauDate())
            //act + arrange
            val event = awaitItem()
            val decision = CIA.makeYourMind(event)

            //assert
            assert(decision is TransferingDecision.CREATEFILE)
            assert(decision?.filePath == toto.fullPath)
            assert((decision as TransferingDecision.CREATEFILE).modificationDate == 817L.toTauDate())
        }
    }

    @Test
    fun `SpyService envoie une image de dossier au démarrage`() = runTest {

        //* SPY ----   events on items   ---->  FBI ---- treated infos     ----> AIRFORCE
        //  alerté auto. expose flux events --> service: makeYourMind(event) --> envoie à Room

        prepareKoin(testScheduler)

        val fakeRepo: IFolderRepo by inject()
        val fakeCompo: IFolderCompo by inject()
        val fakeVM: TauViewModel by inject()


        //assert
        //* répertoire à observer
        val PATH = "/storage/emulated/0/Download".toTauPath()
        val returnedFolder = FOLDER_FULL(PATH)

        val spy = Spy(StandardTestDispatcher(testScheduler))
        val airForce = AirForce()
//        val decisionLogic = FBI::makeYourMind(spy.DiskEventFlow)
        //spy.startSurveillance()

        every { fakeRepo.readFolder(path: any()) } returnMany listOf(
            TauFolder.EMPTY,
            returnedFolder
        )

        //act
        val toto = FILE_TOTO(PATH)
        val fileToEmit = toto.fullPath

        spy.completeScanFlow.test {

            /**ce qui déclenche un completeScan:
             * 1. au démarrage / lors d'un changeFolder
             *    (cependant cf [[room se mêle du scan de démarrage]])
             * 2. après N diffs
             **/
            spy.setObservedFolder(PATH)


            //act + arrange
            val event = awaitItem()
            val decision = CIA.makeYourMind(event)

            //assert
            assert(decision is TransferingDecision.CREATEFILE)
            assert(decision?.filePath == toto.fullPath)
            assert((decision as TransferingDecision.CREATEFILE).modificationDate == 817L.toTauDate())
        }
    }

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

        spy.incomingEventFlow.test {

            spy.emitFake_CREATEFILE(fileToEmit, ItemType.FILE, 817L.toTauDate())
            //act + arrange
            val event = awaitItem()
            val decision = CIA.makeYourMind(event)

            //assert
            assert(decision is TransferingDecision.CREATEFILE)
            assert(decision?.filePath == toto.fullPath)
            assert((decision as TransferingDecision.CREATEFILE).modificationDate == 817L.toTauDate())
        }
    }
}

