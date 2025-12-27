package lorry.dossiertau.data.dbModel

import androidx.room.TypeConverter
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId

class FileIdConverter {

    @TypeConverter
    fun fromFileId(v: FileId?): String? = when (v) {
        null -> null
        FileId.EMPTY -> "EMPTY"
        is FileId.FileIdValue -> "${v.dev}:${v.ino}"
    }

    @TypeConverter
    fun toFileId(s: String?): FileId? = when {
        s == null -> null
        s == "EMPTY" -> FileId.EMPTY
        else -> {
            val (dev, ino) = s.split(":")
            FileId.fileIdOf(dev.toLong(), ino.toLong())
        }
    }
}
