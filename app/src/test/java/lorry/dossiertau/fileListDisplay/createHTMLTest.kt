package lorry.dossiertau.fileListDisplay

import androidx.compose.ui.Modifier.Companion.any
import com.google.common.base.CharMatcher.any
import data.ftp.FtpDS
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import lorry.dossiertau.data.dbModel.AppDb
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.usecases.generateHTMLs.Links
import lorry.dossiertau.usecases.generateHTMLs.VmLinks
import lorry.dossiertau.usecases.generateHTMLs.repos.IDiskRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.INasRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.IWebScrappingRepo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner
import kotlin.collections.listOf
import kotlin.suspend


@RunWith(RobolectricTestRunner::class)
class createHTMLTest : KoinTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    var db: AppDb? = null
    var dbDao: FileDiffDao? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#18 Links - rename file #1 - nothing to do`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()
        val ftpDs = mock<FtpDS>()

        val originalVideoFileName = "threesomes & foursomes.bonnge.three.machin.mp4".toTauFileName()

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo,
            ftpDS = ftpDs,
        )

        val siteActresses = listOf(
            gee()
        )

        val localActresses = listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )

        val localSubjects = listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        val movieSubjects = listOf(
            trio()
        )

        //arrange
        everySuspend { diskRepo.getVideoActresses() } returns localActresses
        everySuspend { diskRepo.getVideoSubjects() } returns localSubjects

        everySuspend { nasRepo.getVideoNames() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getWebPageActresses(
            name = originalVideoFileName,
            localActresses = listOf()
        ) } returns Pair("", listOf("bonnge"))

        everySuspend {
            webScrappingRepo.getWebPageSubjects(
                name = originalVideoFileName,
                movieHtml = "",
                localSubjects = movieSubjects
                )
        } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        everySuspend {
            ftpDs.createHtmlInAnnexes(
                fileName = any<TauItemName>(),
                textContent = any<String>()
            )
        } calls suspend { true }

        everySuspend {
            ftpDs.exists(
                localFilePath = any<TauPath>(),
                fileName = any<TauItemName>()
            )
        } returns false

        everySuspend {
            diskRepo.deleteAllHtmlsIn(
                root = any<TauPath>()
            )
        } calls {}

        everySuspend {
            ftpDs.readJpgFromFtpAsBase64(
                fileName = any<TauItemName>()
            )
        } returns ""

        everySuspend {
            ftpDs.readDescriptionFromFtpAsBase64(
                fileName = any<TauItemName>()
            )
        } returns ""

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoNames() }
        verifySuspend(exactly(1)) { diskRepo.getVideoActresses() }
        verifySuspend(exactly(1)) { diskRepo.getVideoSubjects() }
        verifySuspend(exactly(1)) {
            webScrappingRepo.getWebPageActresses(name = originalVideoFileName,
                localActresses = localActresses)
        }
        verifySuspend(exactly(1)) {
            webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,
                movieHtml = "",
                localSubjects = localSubjects)
        }
        verifySuspend(exactly(0)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = originalVideoFileName
            )
        }
    }

    /*
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#19 Links ∎ rename file #2 ∎ one actress`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()

        val originalVideoFileName = "threesomes & foursomes.bonnge.three.machin.mp4".toTauFileName()
        val finalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.mp4".toTauFileName()

        val ftpDs = mock<FtpDS>()
        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo,
            ftpDS = ftpDs,
        )

        //arrange
        everySuspend { diskRepo.getVideoActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getVideoSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoNames() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) } returns listOf(
            morgan().name
        )
        everySuspend { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoNames() }
        verifySuspend(exactly(1)) { diskRepo.getVideoActresses() }
        verifySuspend(exactly(1)) { diskRepo.getVideoSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = finalVideoFileName
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#20 Links ∎ rename file #3 ∎ 2 actresses`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()

        val originalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.mp4".toTauFileName()
        val finalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.janaco.lanarh.mp4".toTauFileName()

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo
        )

        //arrange
        everySuspend { diskRepo.getVideoActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getVideoSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoNames() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) } returns listOf(
            cova().name,
            rhoades().name
        )
        everySuspend { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoNames() }
        verifySuspend(exactly(1)) { diskRepo.getVideoActresses() }
        verifySuspend(exactly(1)) { diskRepo.getVideoSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = finalVideoFileName
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#21 Links ∎ rename file #4 ∎ 2 actresses whom 1 exists`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()

        val originalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.mp4".toTauFileName()
        val finalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.lanarh.mp4".toTauFileName()

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo
        )

        //arrange
        everySuspend { diskRepo.getVideoActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getVideoSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoNames() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) } returns listOf(
            morgan().name,
            rhoades().name
        )
        everySuspend { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoNames() }
        verifySuspend(exactly(1)) { diskRepo.getVideoActresses() }
        verifySuspend(exactly(1)) { diskRepo.getVideoSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = finalVideoFileName
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#22 Links ∎ rename file #5 ∎ 1 subject`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()

        val originalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.mp4".toTauFileName()
        val finalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.bando.mp4".toTauFileName()

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo
        )

        //arrange
        everySuspend { diskRepo.getVideoActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getVideoSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoNames() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) } returns listOf()
        everySuspend { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) } returns listOf(
            bandeau().name
        )

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoNames() }
        verifySuspend(exactly(1)) { diskRepo.getVideoActresses() }
        verifySuspend(exactly(1)) { diskRepo.getVideoSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = finalVideoFileName
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#23 Links ∎ rename file #5 ∎ 2 subjects`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()

        val originalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.mp4".toTauFileName()
        val finalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.bando.black.mp4".toTauFileName()

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo
        )

        //arrange
        everySuspend { diskRepo.getVideoActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getVideoSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoNames() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) } returns listOf()
        everySuspend { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) } returns listOf(
            bandeau().name, black().name
        )

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoNames() }
        verifySuspend(exactly(1)) { diskRepo.getVideoActresses() }
        verifySuspend(exactly(1)) { diskRepo.getVideoSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = finalVideoFileName
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#24 Links ∎ rename file #5 ∎ 1 subject + 1 actress`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()

        val originalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.mp4".toTauFileName()
        val finalVideoFileName =
            "threesomes & foursomes.bonnge.three.machin.markmo.janaco.black.mp4".toTauFileName()

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo
        )

        //arrange
        everySuspend { diskRepo.getVideoActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getVideoSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoNames() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) } returns listOf(cova().name)
        everySuspend { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) } returns listOf(black().name)

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoNames() }
        verifySuspend(exactly(1)) { diskRepo.getVideoActresses() }
        verifySuspend(exactly(1)) { diskRepo.getVideoSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageActresses(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) { webScrappingRepo.getWebPageSubjects(name = originalVideoFileName,) }
        verifySuspend(exactly(1)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = finalVideoFileName
            )
        }
    }

     */

}