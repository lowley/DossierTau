package lorry.dossiertau.fileListDisplay

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import lorry.basics.TauInjections
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.sameContentAs
import lorry.dossiertau.data.transfer.TauRepoFile
import lorry.dossiertau.data.transfer.TauRepoFolder
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.usecases.folderContent.FolderCompo
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.junit.After
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

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

    private fun prepareKoin(testScheduler: TestCoroutineScheduler) {
        startKoin {
            modules(
                TauInjections,
                module {
                    allowOverride(true)
                    single<IFolderRepo> { spyk(get<IFolderRepo>(named("real"))) }
                    single<IFolderCompo> { spyk(FolderCompo(get(), StandardTestDispatcher(testScheduler))) }
                    single<TauViewModel> { spyk(TauViewModel(get())) }
                }
            )
        }
    }

    fun FILE_TOTO(parentPath: TauPath) = TauFile(
        parentPath = parentPath,
        name = TauItemName("toto.mp4"),
        modificationDate = TauDate.fromLong(825)
    )

    fun REPOFILE_TOTO(parentPath: TauPath) = TauRepoFile(
        parentPath = parentPath,
        name = TauItemName("toto.mp4"),
        modificationDate = TauDate.fromLong(825)
    )

    fun FOLDER_DIVERS(parentPath: TauPath) = TauFolder(
        fullPath = TauPath.of("$parentPath/divers"),
        modificationDate = TauDate(834)
    )

    fun REPOFOLDER_DIVERS(parentPath: TauPath) = TauRepoFolder(
        fullPath = TauPath.of("$parentPath/divers"),
        modificationDate = TauDate(834)
    )

    @After
    fun tearDownKoin() {
        stopKoin()
    }
}

