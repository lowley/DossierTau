package lorry.dossiertau.usecases.naming

import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.Subject
import org.junit.Assert.assertEquals
import org.junit.Test

class NamingTest {

    @Test
    fun `ajoute les shortcuts canoniques des elements connus`() {
        val result = Naming.compute(
            Naming.Request(
                originalFileName = "movie.mp4",
                pageActresses = listOf("Jane Doe"),
                pageSubjects = listOf("blindfold"),
                localActresses = listOf(
                    Actress(name = "jane doe", shortcuts = listOf("jane", "jane doe"))
                ),
                localSubjects = listOf(
                    Subject(name = "bandeau", shortcuts = listOf("bandeau", "blindfold"))
                ),
            )
        )

        assertEquals("movie.jane.bandeau.mp4", result.fileName)
        assertEquals(listOf("jane doe"), result.matchedActresses.map { it.name })
        assertEquals(listOf("bandeau"), result.matchedSubjects.map { it.name })
    }

    @Test
    fun `ignore les personnes et sujets absents du referentiel local`() {
        val result = Naming.compute(
            Naming.Request(
                originalFileName = "movie.mp4",
                pageActresses = listOf("Unknown Person"),
                pageSubjects = listOf("unknown subject"),
                localActresses = listOf(
                    Actress(name = "jane doe", shortcuts = listOf("jane", "jane doe"))
                ),
                localSubjects = listOf(
                    Subject(name = "bandeau", shortcuts = listOf("bandeau", "blindfold"))
                ),
            )
        )

        assertEquals("movie.mp4", result.fileName)
    }

    @Test
    fun `le renommage est idempotent`() {
        val actress = Actress(name = "jane doe", shortcuts = listOf("jane", "jane doe"))

        val once = Naming.compute(
            Naming.Request(
                originalFileName = "movie.mp4",
                pageActresses = listOf("jane doe"),
                localActresses = listOf(actress),
            )
        ).fileName

        val twice = Naming.compute(
            Naming.Request(
                originalFileName = once,
                pageActresses = listOf("jane doe"),
                localActresses = listOf(actress),
            )
        ).fileName

        assertEquals("movie.jane.mp4", once)
        assertEquals(once, twice)
    }
}
