package lorry.dossiertau.support.littleClasses


/////////////////////////
// value class : τPath //
/////////////////////////
@JvmInline
value class TauPath(val value: String){

    fun normalize(): TauPath{

        if (this.value.isEmpty())
            return this

        var normalized = this.value.trim()
            .replace("//", "/")
            .replace("\\", "/")

        if (!normalized.endsWith("/"))
            normalized = "$normalized/"

        if (!normalized.startsWith("/"))
            normalized = "/$normalized"

        return TauPath(normalized)
    }

    override fun toString(): String = "τPath($value)"
    fun equalsTo(other: TauPath) = this.normalize().value == other.normalize().value

    companion object{
        val EMPTY = TauPath("")
    }

}

fun String.toTauPath() = TauPath(this).normalize()

inline val TauPath.parentPath
    get() = this.normalize().value.substringBeforeLast("/").toTauPath()

inline val TauPath.name
    get() = this.normalize().value.substringAfterLast("/").toTauFileName()
