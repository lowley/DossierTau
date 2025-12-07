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
            else -> Data(normalize(value)).right().toTauPath()
        }

        private fun normalize(value: String): String {
            var t = value.trim().replace("\\", "/").replace("//", "/")
            if (!t.endsWith("/")) t += "/"
            if (!t.startsWith("/")) t = "/$t"
            return t
        }
    }


    override fun toString(): String = when (this.value) {
        is Either.Right -> "τPath(${value.right().getOrNull()?.value?.value ?: "EMPTY/PB"})"
        is Either.Left -> "τPath(EMPTY)"
    }

    fun equalsTo(other: TauPath) = {
        val result = when (listOf(this.value::class.java, other.value::class.java)) {
            listOf(EMPTY::class.java, EMPTY::class.java) -> true
            listOf(EMPTY::class.java, Data::class.java) -> false
            listOf(Data::class.java, EMPTY::class.java) -> false
            else -> {
                val mine = this.value as Data
                val theirs = other.value as Data
                mine == theirs
            }
        }
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
                (it.value + end).toTauPath()
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
                (it.value + end.value).toTauPath()
            }
        )

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
