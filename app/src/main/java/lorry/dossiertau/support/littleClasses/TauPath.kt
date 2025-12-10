package lorry.dossiertau.support.littleClasses

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import lorry.dossiertau.support.littleClasses.TauPath.Companion.Data
import lorry.dossiertau.support.littleClasses.TauPath.Companion.EMPTY
import java.io.File

typealias EMPTY = TauPath.Companion.EMPTYTAG

//////////////////////////
// value class : τPath2 //
//////////////////////////
@JvmInline
value class TauPath(val value: Either<EMPTY, Data>) {

    companion object {
        object EMPTYTAG
        val EMPTY = TauPath(EMPTYTAG.left())

        data class Data(val value: String)

        fun of(value: String): TauPath = when (value) {
            "" -> EMPTY
            else -> Data(normalizeWithoutSlash(value)).right().toTauPath()
        }

        fun normalizeWithoutSlash(value: String): String {
            var t = value.trim().replace("\\", "/").replace("//", "/")
            if (t.endsWith("/")) t = t.dropLast(1)
            if (!t.startsWith("/")) t = "/$t"
            return t
        }

        fun normalizeWithSlash(value: String): String {
            var t = value.trim().replace("\\", "/").replace("//", "/")
            if (!t.endsWith("/")) t += "/"
            if (!t.startsWith("/")) t = "/$t"
            return t
        }
    }


    override fun toString(): String = when (val e = this.value) {
        is Either.Right -> "τPath(${e.value.value})"
        is Either.Left -> "τPath(EMPTY)"
    }

    infix fun equalsTo(other: TauPath): Boolean =
        when {
            this.value is Either.Left && other.value is Either.Left -> true
            this.value is Either.Right && other.value is Either.Right ->
                (this.value as Either.Right).value.value ==
                        (other.value as Either.Right).value.value
            else -> false
        }

    fun toFile(): Option<File> {
        return when (this.value) {
            is Either.Left -> Option.fromNullable(null)
            is Either.Right -> Option.fromNullable(File(this.value.value.value))
        }
    }

    fun appendToTauPath(end: String): TauPath {
        val result = this.value.fold(
            ifLeft = {
                end.toTauPath()
            },
            ifRight = {

                (normalizeWithSlash(it.value) + end).toTauPath()
            }
        )

        return result
    }

    fun appendToTauPath(end: TauItemName): TauPath {
        val result = this.value.fold(
            ifLeft = {
                end.value.toTauPath()
            },
            ifRight = {
                "${it.value}/${end.value}".toTauPath()
            }
        )

        return result
    }

    fun normalizeWithoutSlash(): TauPath =
        when (val e = this.value) {
            is Either.Left  -> EMPTY
            is Either.Right -> TauPath.of(Companion.normalizeWithoutSlash(e.value.value))
        }

    fun normalizeWithSlash(): TauPath {
        val result = when (val e = this.value) {
            is Either.Left -> EMPTY
            is Either.Right -> normalizeWithSlash(e.getOrNull()!!.value).toTauPath()
        }

        return result
    }
}

fun Either<EMPTY, Data>.toTauPath() = TauPath(this)
fun String.toTauPath() = TauPath.of(this)

inline val TauPath.parentPath
    get() = when (this.value) {
        is Either.Left -> EMPTY
        is Either.Right -> this.value.value.value.substringBeforeLast("/").toTauPath()
    }


inline val TauPath.name
    get() = when (this.value) {
        is Either.Left -> "".toTauFileName()
        is Either.Right -> this.value.value.value.substringAfterLast("/").toTauFileName()
    }

inline val TauPath.path
    get() = when (this.value) {
        is Either.Left -> ""
        is Either.Right -> this.value.value.value
    }

