package lorry.dossiertau.fileListDisplay

import arrow.core.raise.fold
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FileListDisplayTests {

    @Test
    fun `TauViewModel demande récupération contenu d'un répertoire`() = runTest {


        //* input
        val PATH = TauPath("/storage/emulated/0/Download")
        val diskItems = listOf(
            TauFile.dummywith {
                name = TauName("toto"),
                extension = TauExtension("mp4")
            },
            TauFolder.dummyWith {
                name = TauName("divers"),
                fullpath = TauPath("$PATH/divers")
            }
        )

        //* output


        //arr
        val folderExplorer: IFolderExplorer = mockk<IFolderExplorer>()
        val viewModel: TauViewModel = spyk<TauViewModel>(TauViewModel(folderExplorer()))

        folderExplorer.folderContentFlow.test {

            every { folderExplorer.getItemsOfPath(TauPath(PATH)) } returns diskItems

            //act
            viewModel.setFolderPath(path = PATH)

            //ass
            verify { folderExplorer.getItemsOfPath(TauPath(PATH)) }
            val newItems = awaitItem()
            assert(newItems == diskItems)


        }


    }


}







}