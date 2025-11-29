package lorry.dossiertau.support.littleClasses

import arrow.core.Either
import arrow.core.Ior
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import lorry.dossiertau.support.littleClasses.TauPath2.Companion.Data2
import lorry.dossiertau.support.littleClasses.TauPath2.Companion.EMPTY2
import java.io.File

//////////////////////////
// value class : τPath2 //
//////////////////////////
@JvmInline
value class TauPath2(val value: Either<EMPTY2, Data2>) {

    companion object {
        object EMPTY2
        data class Data2(val value: String)

        fun of(value: String): TauPath2 = when (value) {
            "" -> EMPTY2.left().toTauPath2()
            else -> Data2(normalize(value)).right().toTauPath2()
        }

        private fun normalize(value: String): String {
            var t = value.trim().replace("\\", "/").replace("//", "/")
            if (!t.endsWith("/")) t += "/"
            if (!t.startsWith("/")) t = "/$t"
            return t
        }
    }


    override fun toString(): String = when (this.value) {
        is Either.Right -> "τPath($value)"
        is Either.Left -> "τPath(EMPTY)"
    }

    fun equalsTo(other: TauPath2) = {
        val result = when (listOf(this.value::class.java, other.value::class.java)) {
            listOf(EMPTY2::class.java, EMPTY2::class.java) -> true
            listOf(EMPTY2::class.java, Data2::class.java) -> false
            listOf(Data2::class.java, EMPTY2::class.java) -> false
            else -> {
                val mine = this.value as Data2
                val theirs = other.value as Data2
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
}


fun Either<EMPTY2, Data2>.toTauPath2() = TauPath2(this)
fun String.toTauPath2() = TauPath2.of(this)


inline val TauPath2.parentPath
    get() = when (this.value) {
        is Either.Left -> EMPTY2.left().toTauPath2()
        is Either.Right -> this.value.value.value.substringBeforeLast("/").toTauPath2()
    }


inline val TauPath2.name
    get() = when (this.value) {
        is Either.Left -> "".toTauFileName()
        is Either.Right -> this.value.value.value.substringAfterLast("/").toTauFileName()
    }
