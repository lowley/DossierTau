package lorry.dossiertau.support.littleClasses

@JvmInline
value class TauItemName(val value: String) {

    override fun toString(): String = "τFileName($value)"

    fun equalsTo(other: TauItemName) = this.value == other.value

    fun hasExtension() = this.value.contains(".")
    fun hasNoExtension() = !this.hasExtension()

    companion object{
        val EMPTY = TauItemName("")
    }


}

fun String.toTauFileName() = TauItemName(this)

inline val TauItemName.extension
    get() = this.value.substringAfterLast(".")