package lorry.dossiertau.support.littleClasses

import arrow.core.Option
import java.io.File


/////////////////////////
// value class : τPath //
/////////////////////////
sealed class TauPath {

    data class Data(val value: String) : TauPath()
    data object EMPTY : TauPath()

    companion object {
        fun of(value: String) = when (value) {
            "" -> EMPTY
            else -> Data(value)
        }
    }

    fun normalize(): TauPath {

        if (this is EMPTY)
            return this

        var normalized = (this as Data).value.trim()
            .replace("//", "/")
            .replace("\\", "/")

        if (!normalized.endsWith("/"))
            normalized = "$normalized/"

        if (!normalized.startsWith("/"))
            normalized = "/$normalized"

        return TauPath.Data(normalized)
    }

    override fun toString(): String = when (this) {
        is Data -> "τPath($value)"
        is EMPTY -> "τPath(EMPTY)"
    }


    inline fun equalsTo(other: TauPath) = {
        when (listOf(this::class.java, other::class.java)) {
            listOf(EMPTY::class.java, EMPTY::class.java) -> true
            listOf(EMPTY::class.java, Data::class.java) -> false
            listOf(Data::class.java, EMPTY::class.java) -> false
            else -> {
                val mine = this as Data
                val theirs = other as Data
                mine.normalizeData() == other.normalizeData()
            }
        }
    }

    fun toFile(): Option<File> {
        return when (this) {
            is EMPTY -> Option.fromNullable(null)
            is Data -> Option.fromNullable(File(value))
        }
    }
}

fun String.toTauPath() = when (this) {
    "" -> TauPath.EMPTY
    else -> TauPath.Data(this).normalizeData()
}

inline val TauPath.parentPath
    get() = when (this) {
        is TauPath.EMPTY -> TauPath.EMPTY
        is TauPath.Data -> this.normalizeData().value.substringBeforeLast("/").toTauPath()
    }


inline val TauPath.name
    get() = when (this) {
        is TauPath.EMPTY -> "".toTauFileName()
        is TauPath.Data -> this.normalizeData().value.substringAfterLast("/").toTauFileName()
    }

fun TauPath.Data.normalizeData(): TauPath.Data {

    var normalized = this.value.trim()
        .replace("//", "/")
        .replace("\\", "/")

    if (!normalized.endsWith("/"))
        normalized = "$normalized/"

    if (!normalized.startsWith("/"))
        normalized = "/$normalized"

    return TauPath.Data(normalized)
}
