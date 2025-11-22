package lorry.dossiertau.support.littleClasses

@JvmInline
value class TauExtension(val value: String) {

    override fun toString(): String = "τExtension($value)"

    fun equalsTo(other: TauExtension) = this.value == other.value

    companion object{
        val EMPTY = TauExtension("")
    }



}

fun String.toTauExtension() = if (this.isEmpty())
    TauExtension.EMPTY else TauExtension(this)
