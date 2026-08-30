package lorry.dossiertau.fileListDisplay

import app.cash.turbine.testIn
import ch.tutteli.atrium.api.fluent.en_GB.feature
import ch.tutteli.atrium.api.fluent.en_GB.notToEqualNull
import ch.tutteli.atrium.api.fluent.en_GB.toBeAnInstanceOf
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toHaveSize
import ch.tutteli.atrium.api.verbs.expect
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import lorry.dossiertau.data.dbModel.AppDb
import lorry.dossiertau.data.dbModel.DiffRepository
import lorry.dossiertau.data.dbModel.FileDiffDao
import lorry.dossiertau.data.intelligenceService.AirForce
import lorry.dossiertau.data.intelligenceService.CIA
import lorry.dossiertau.data.intelligenceService.utils2.events.Snapshot
import lorry.dossiertau.data.intelligenceService.utils2.events.SnapshotElement
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.model.fileId
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauPath
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner
import lorry.dossiertau.usecases.generateHTMLs.toBitmap
import lorry.dossiertau.usecases.generateHTMLs.toByteArray
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class GlobalScanWithContentTest : KoinTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    var db: AppDb? = null
    var dbDao: FileDiffDao? = null

//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun `#1 ScanAndFiles ∎ dossierSuivi change ∎ ⇒ verif envoi`() = runTest {
//
//        val dispatcher = StandardTestDispatcher(testScheduler)
//
//        TestStuff.configure(dispatcher).use { stuff ->
//            val (repo, compo, vm, spy, dbDao, spyRepo) = stuff
//
//            setAsInjectors(repo, compo, vm, spy, dbDao, testScheduler, spyRepo)
//
//            val spyFlow = spy.spyLevelFlow.testIn(this)
//            val dbFlow = dbDao.diffFlow().drop(1).testIn(this)
//
//            val cia = CIA()
//            cia.spy = spy
//
//            val PATH = "/storage/emulated/0/Download".toTauPath()
//
//            advanceUntilIdle()
//            val global = spyFlow.awaitItem()
//
//            val diffRepo = DiffRepository(
//                dao = dbDao,
//                io = dispatcher
//            )
//
//            val testScope = this
//            val airForce = AirForce(
//                repo = diffRepo,
//                scope = testScope
//            )
//
//            airForce.cia = cia
//            val job = airForce.startListeningForCIADecisions()
//
//            val byteArray = ByteArray(18)
//            byteArray.set(15, 13)
//            val bitmap = byteArray.toBitmap()
//
//            val downloads = "/storage/emulated/0/Download".toTauPath()
//
//            //l'ancien snapshot
//            spy.snapshotsAtomic.set(
//                Snapshot(
//                    folderPath = downloads,
//                    entriesByName = mapOf()
//                )
//            )
//
//            //le nouveau snapshot
//            val element = SnapshotElement(
//                name = "colomba.txt",
//                isDir = false,
//                size = 18L,
//                lastModified = 3815L,
//                fileId = FileId.fileIdOf(5L, 45L),
//                picture = TauPicture.fromBitmap(bitmap),
//                memo = "corps liquide"
//            )
//
//            everySuspend { repo.createSnapshotFor(PATH) } returns Snapshot(
//                folderPath = downloads,
//                entriesByName = mapOf(
//                    "colomba.txt" to element
//                )
//            )
//
//            //act
//            spy.setObservedFolder(PATH)
//
//            //act + arrange
//            advanceTimeBy(500)
//            runCurrent()
//            val event = spyFlow.awaitItem()
//            val decision = cia.manageUpdateEvents(event)
//            cia.emitCIALevels(decision)
//
//            advanceTimeBy(500)
//            runCurrent()
//            val entry = dbFlow.awaitItem()
//            println("afterInsert = $entry")
//
//            expect(entry).notToEqualNull() {
//                toBeAnInstanceOf<Content>()
//                feature { f((it as Content)::full_path) }.toEqual(downloads.path)
//                feature { f((it as Content)::items) }.toHaveSize(1)
////                feature { f((it as Content)::correlationId) }.toEqual(folderToEmit.path)
////                feature { f((it as Content)::modifiedAtIso) }.toEqual(folderToEmit.path)
////                feature { f((it as Content)::fileId) }.toEqual(folderToEmit.path)
//            }
//
//            expect((entry as Content).items[0]) {
//                toBeAnInstanceOf<ContentItem>()
////                feature { f((it as ContentItem)::id) }.toEqual(folderToEmit.path)
//                feature { f(it::name) }.toEqual(element.name)
//                feature { f(it::picture) }.toEqual(element.picture?.bitmap?.toByteArray())
//                feature { f(it::memo) }.toEqual(element.memo)
//                feature { f(it::fileId) }.toEqual(element.fileId)
//                feature { f(it::modificationDate) }.toEqual(Instant.ofEpochMilli(element.lastModified))
//            }
//
//            job.cancel()
//            spyFlow.cancel()
//            dbFlow.cancel()
//        }
//    }
}