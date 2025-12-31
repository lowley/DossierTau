package lorry.dossiertau.data.dbModel.converters

import androidx.room.TypeConverter
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import java.time.Instant

class DateConverters {

    @TypeConverter
    fun instantToIso(instant: Instant?): String? =
        instant?.toString() // ISO 8601 UTC

    @TypeConverter
    fun isoToInstant(value: String?): Instant? =
        value?.let { Instant.parse(it) }
}

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
            FileId.Companion.fileIdOf(dev.toLong(), ino.toLong())
        }
    }
}