package lorry.dossiertau.fileListDisplay

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.drop
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
import lorry.dossiertau.usecases.folderContent.FolderCompo
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.FolderRepo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.junit.After
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject

class FileListDisplayTests : KoinTest {

    @Test
    fun `IFolderCompo récupère le contenu d'un répertoire`() = runTest {

        prepareKoin()
        val fakeRepo: IFolderRepo by inject()
        val fakeCompo: IFolderCompo by inject()
        val fakeVM: TauViewModel by inject()

        //* input
        val PATH = "/storage/emulated/0/Download"
        val compoItems = listOf(
            TauFile(
                parentPath = TauPath(PATH),
                name = TauItemName("toto.mp4"),
                modificationDate = TauDate.fromLong(825)
            ),
            TauFolder(
                fullPath = TauPath("$PATH/divers"),
                modificationDate = TauDate(834)
            )
        )
        val repoItems = listOf(
            TauRepoFile(
                parentPath = TauPath(PATH),
                name = TauItemName("toto.mp4"),
                modificationDate = TauDate.fromLong(825)
            ),
            TauRepoFolder(
                fullPath = TauPath("$PATH/divers"),
                modificationDate = TauDate(834)
            )
        )

        fakeCompo.folderFlow.drop(1).test {
            every { fakeRepo.getItemsInFullPath(TauPath(PATH)) } returns repoItems

            //act
            fakeVM.setTauFolder(folderPath = TauPath(PATH))

            //ass
            verify { fakeRepo.getItemsInFullPath(TauPath(PATH)) }
            val newItems = awaitItem()
            assert(
                newItems.fold(
                    ifEmpty = { false },
                    ifSome = { folder -> folder.children.sameContentAs(compoItems) }
                ))
        }
    }

    private fun prepareKoin() {

        startKoin {
            modules(
                TauInjections,
                module {
                    allowOverride(true)
                    single<IFolderRepo> { spyk(get<IFolderRepo>(named("real"))) }
                    single<IFolderCompo> { spyk(get<IFolderCompo>(named("real"))) }
                    single<TauViewModel> { spyk(get<TauViewModel>(named("real"))) }
                }
            )
        }
    }

    @After
    fun tearDownKoin() {
        stopKoin()
    }
}

