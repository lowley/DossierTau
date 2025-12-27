package lorry.dossiertau.data.intelligenceService.utils2.repo

import android.system.Os
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId.Companion.fileIdOf
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path

class SpyRepo : ISpyRepo {
    override fun getIdOf(path: TauPath): FileId{
            val s1 = Os.stat(path.path)
            return fileIdOf(dev = s1.st_dev.toLong(), ino = s1.st_ino.toLong())
        }
}