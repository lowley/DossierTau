package lorry.dossiertau.fileListDisplay

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.TauViewModel
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.usecases.folderContent.IFolderCompo
import lorry.dossiertau.usecases.folderContent.support.IFolderRepo
import org.junit.After
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class FileListDisplayTests : KoinTest {

    @Test
    fun `IFolderCompo récupère le contenu d'un répertoire`() = runTest {

        /**
         * @startuml
         * skinparam sequenceMessageAlign center
         * skinparam responseMessageBelowArrow true
         *
         * title Récupération du contenu d'un répertoire
         * boundary TauViewModel
         * entity IFolderCompo #red
         * database IFolderRepo
         * TauViewModel -> IFolderCompo: setTauFolder(TauPath)
         * IFolderCompo -> IFolderRepo: getItemOfPath(TauPath)
         * IFolderCompo <- IFolderRepo: List<TauItem>
         * IFolderCompo -> IFolderCompo: setFolderContentFlow
         * @enduml
         */

        //arr
        val fakeRepoSeed = mockk<IFolderRepo>()
        val fakeCompoSeed = spyk<IFolderCompo>()
        val fakeVMSeed = spyk<TauViewModel>()

        startKoin {
            modules(
                module {
                    single<IFolderRepo> { fakeRepoSeed }
                    single<IFolderCompo> { fakeCompoSeed }
                    single { fakeVMSeed }
                })
        }

        val fakeRepo: IFolderRepo by inject()
        val fakeCompo: IFolderCompo by inject()
        val fakeVM: TauViewModel by inject()

        //* input
        val PATH = TauPath("/storage/emulated/0/Download")
        val compoItems = listOf(
            TauFile(
                name = TauItemName("toto"),
                extension = TauExtension("mp4")
            ),
            TauFolder.dummyWith {
                name = TauItemName("divers"),
                fullpath = TauPath("$PATH/divers")
            }
        )
        val repoItems = listOf(
            TauRepoFile.dummywith {
                name = TauName("toto"),
                extension = TauExtension("mp4")
            },
            TauRepoFolder.dummyWith {
                name = TauName("divers"),
                fullpath = TauPath("$PATH/divers")
            }
        )

        fakeCompoSeed.folderContentFlow.test {

            every { fakeRepo.getItemsOfPath(TauPath(PATH)) } returns repoItems

            //act
            fakeVM.setTauFolder(path = PATH)

            //ass
            verify { fakeRepo.getItemsOfPath(TauPath(PATH)) }
            val newItems = awaitItem()
            assert(newItems == compoItems)
        }
    }

//    @Before
//    fun setupKoin() {
//        startKoin {
//            // ici tu mets tes modules **de test**
//            modules(testInjections)
//        }
//    }

    @After
    fun tearDownKoin() {
        stopKoin()
    }


}

