package lorry.dossiertau.utils

import lorry.dossiertau.fileListDisplay.FileListDisplayTests
import org.koin.dsl.module

val testInjections = module {

    single<IFolderRepo> { FileListDisplayTests.fakeFolderRepo }
    single<IFolderCompo> { FileListDisplayTests.fakeFolderCompo }
    single { FileListDisplayTests.fakeVMSeed }



//    single<IParser> { Parser() }
//    single { LogBuilder(get()) }
}