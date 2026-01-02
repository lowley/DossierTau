package lorry.dossiertau.fileListDisplay

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


@RunWith(RobolectricTestRunner::class)
class createHTMLTest : KoinTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    var db: AppDb? = null
    var dbDao: FileDiffDao? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `#18 Links ∎ rename file #1 ∎ nothing to do`() = runTest {

        val dispatcher = StandardTestDispatcher(testScheduler)

        val vmLinks = VmLinks()
        val nasRepo = mock<INasRepo>()
        val diskRepo = mock<IDiskRepo>()
        val webScrappingRepo = mock<IWebScrappingRepo>()

        val originalVideoFileName = "threesomes & foursomes.bonnge.three.machin.mp4".toTauFileName()

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo
        )

        //arrange
        everySuspend { diskRepo.getLocalActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getLocalSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoPaths() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getMovieActresses(name = originalVideoFileName) } returns listOf()
        everySuspend { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoPaths() }
        verifySuspend(exactly(1)) { diskRepo.getLocalActresses() }
        verifySuspend(exactly(1)) { diskRepo.getLocalSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieActresses(name = originalVideoFileName) }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) }
        verifySuspend(exactly(0)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = originalVideoFileName
            )
        }
    }

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

        val links = Links(
            vm = vmLinks,
            nasRepo = nasRepo,
            diskRepo = diskRepo,
            webScrappingRepo = webScrappingRepo
        )

        //arrange
        everySuspend { diskRepo.getLocalActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getLocalSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoPaths() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getMovieActresses(name = originalVideoFileName) } returns listOf(
            morgan()
        )
        everySuspend { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoPaths() }
        verifySuspend(exactly(1)) { diskRepo.getLocalActresses() }
        verifySuspend(exactly(1)) { diskRepo.getLocalSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieActresses(name = originalVideoFileName) }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) }
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
        everySuspend { diskRepo.getLocalActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getLocalSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoPaths() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getMovieActresses(name = originalVideoFileName) } returns listOf(
            cova(),
            rhoades()
        )
        everySuspend { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoPaths() }
        verifySuspend(exactly(1)) { diskRepo.getLocalActresses() }
        verifySuspend(exactly(1)) { diskRepo.getLocalSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieActresses(name = originalVideoFileName) }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) }
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
        everySuspend { diskRepo.getLocalActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getLocalSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoPaths() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getMovieActresses(name = originalVideoFileName) } returns listOf(
            morgan(),
            rhoades()
        )
        everySuspend { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) } returns listOf()

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoPaths() }
        verifySuspend(exactly(1)) { diskRepo.getLocalActresses() }
        verifySuspend(exactly(1)) { diskRepo.getLocalSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieActresses(name = originalVideoFileName) }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) }
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
        everySuspend { diskRepo.getLocalActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getLocalSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoPaths() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getMovieActresses(name = originalVideoFileName) } returns listOf()
        everySuspend { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) } returns listOf(
            bandeau()
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
        verifySuspend(exactly(1)) { nasRepo.getVideoPaths() }
        verifySuspend(exactly(1)) { diskRepo.getLocalActresses() }
        verifySuspend(exactly(1)) { diskRepo.getLocalSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieActresses(name = originalVideoFileName) }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) }
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
        everySuspend { diskRepo.getLocalActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getLocalSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoPaths() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getMovieActresses(name = originalVideoFileName) } returns listOf()
        everySuspend { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) } returns listOf(
            bandeau(), black()
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
        verifySuspend(exactly(1)) { nasRepo.getVideoPaths() }
        verifySuspend(exactly(1)) { diskRepo.getLocalActresses() }
        verifySuspend(exactly(1)) { diskRepo.getLocalSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieActresses(name = originalVideoFileName) }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) }
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
        everySuspend { diskRepo.getLocalActresses() } returns listOf(
            morgan(),
            cova(),
            rhoades(),
            gee()
        )
        everySuspend { diskRepo.getLocalSubjects() } returns listOf(
            trio(),
            lesbos(),
            bandeau(),
            black()
        )

        everySuspend { nasRepo.getVideoPaths() } returns listOf(originalVideoFileName)

        everySuspend { webScrappingRepo.getMovieActresses(name = originalVideoFileName) } returns listOf(cova())
        everySuspend { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) } returns listOf(black())

        everySuspend {
            nasRepo.renameFile(
                from = any<TauItemName>(),
                to = any<TauItemName>()
            )
        } calls {}

        //act
        links.generateLinks()

        //assert
        verifySuspend(exactly(1)) { nasRepo.getVideoPaths() }
        verifySuspend(exactly(1)) { diskRepo.getLocalActresses() }
        verifySuspend(exactly(1)) { diskRepo.getLocalSubjects() }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieActresses(name = originalVideoFileName) }
        verifySuspend(exactly(1)) { webScrappingRepo.getMovieSubjects(name = originalVideoFileName) }
        verifySuspend(exactly(1)) {
            nasRepo.renameFile(
                from = originalVideoFileName,
                to = finalVideoFileName
            )
        }
    }


}