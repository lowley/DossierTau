package lorry.dossiertau.usecases.generateHTMLs.support

data class Actress(
    val name: String,
    val shortcuts: List<String>
){
    //#[[égalité des Actress]]
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Actress

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

data class Subject(
    val name: String,
    val shortcuts: List<String>
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Subject

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}