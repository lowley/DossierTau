package lorry.dossiertau.support.littleClasses

@JvmInline
value class TauDate(val value: Long) {

    fun toddMMyyyy(): String {
        val date = java.util.Date(value)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy")
        return format.format(date)
    }

    fun toddMMyyyyHHmmss(): String {
        val date = java.util.Date(value)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm:ss")
        return format.format(date)
    }

    fun compareTo(other: TauDate): Int {
        return value.compareTo(other.value)
    }

    fun equalsTo(other: TauDate): Boolean {
        return value == other.value
    }

    companion object{

        fun fromLong(value: Long): TauDate = TauDate(value)
        fun now(): TauDate = TauDate(System.currentTimeMillis())
    }
}