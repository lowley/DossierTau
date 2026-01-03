package lorry.dossiertau.usecases.generateHTMLs.support

interface Stuff{
    val name: String
    val shortcuts: List<String>
}

typealias ActressName = String

data class Actress(
    override val name: ActressName,
    override val shortcuts: List<String>
): Stuff{

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
    override val name: String,
    override val shortcuts: List<String>
): Stuff{

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