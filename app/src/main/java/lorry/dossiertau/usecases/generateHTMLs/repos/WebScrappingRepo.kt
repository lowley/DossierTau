package lorry.dossiertau.usecases.generateHTMLs.repos

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.ui.AppBus
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.ActressName
import lorry.dossiertau.usecases.generateHTMLs.support.MoviesApi
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.HttpException
import retrofit2.Retrofit
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.Locale
import kotlin.collections.emptyList

typealias MovieSuffix = String
typealias MovieHtml = String


class WebScrappingRepo : IWebScrappingRepo {

    val login = "Pvc7NXwy6y7r33YurTuDoZ89"
    val password = "gKVRhVNy7gfjejv6qbrTVX4R"
    val server = "se.socks.nordhold.net"
    val port = 1080

    var moviesApi: MoviesApi? = null

    override suspend fun getMovieActresses(
        movieName: TauItemName,
        localActresses: List<Actress>
    ): Pair<MovieHtml, List<ActressName>> {

        val shortMovieName =
            movieName.value.substringBefore('(').removePunctuation().substringBefore(" by ").trim()
        val allMoviesPageHtml = searchMoviesWithName(shortMovieName)
        val movieSuffixes = searchTheGoodMovies(shortMovieName, allMoviesPageHtml)
        val peopleAndHtmlForSameNameMovies = getInfosOfGoodMovies(movieSuffixes)

        var movieThings: Option<Pair<MovieHtml, List<ActressName>>> = None
        if (peopleAndHtmlForSameNameMovies.size > 1) {
            println("SCRAP trouvés ${peopleAndHtmlForSameNameMovies.size} films avec ce titre. Recherche avec les noms d'actrices ...")
            AppBus.lines.tryEmit("SCRAP trouvés ${peopleAndHtmlForSameNameMovies.size} films avec ce titre. Recherche avec les noms d'actrices ...")

            movieThings = findMovieAmongMovies(
                peopleAndHtmlForSameNameMovies = peopleAndHtmlForSameNameMovies,
                localActresses = localActresses,
                movieName = movieName
            )

            println("SCRAP ... gagnant: ${movieThings.fold({"aucun: des noms d'actrices (dans le fichier) inconnus?"}, {"oui, un"})}")
            AppBus.lines.tryEmit("SCRAP ... gagnant: ${movieThings.fold({"aucun: des noms d'actrices (dans le fichier) inconnus?"}, {"oui, un"})}")

            println("SCRAP actrices: ${peopleAndHtmlForSameNameMovies.values.first()
                .second.joinToString(", ")}")
            AppBus.lines.tryEmit("SCRAP actrices: ${peopleAndHtmlForSameNameMovies.values.first()
                .second.joinToString(", ")}")
        }
        else if (peopleAndHtmlForSameNameMovies.size == 1){
            println("SCRAP trouvés; 1 film avec ce titre. On le prend")
            AppBus.lines.tryEmit("SCRAP trouvés; 1 film avec ce titre. On le prend")

            println("SCRAP actrices: ${peopleAndHtmlForSameNameMovies.values.first()
                .second.joinToString(", ")}")
            AppBus.lines.tryEmit("SCRAP actrices: ${peopleAndHtmlForSameNameMovies.values.first()
                .second.joinToString(", ")}")

            movieThings = peopleAndHtmlForSameNameMovies.values.first().toOption()
        }

        val people = movieThings.fold(
            ifEmpty = { ("" as MovieHtml) to emptyList<ActressName>() },
            ifSome = { thing ->
                thing.first to searchPeople(thing.first)
            }
        )

        return people
    }

    private fun findMovieAmongMovies(
        peopleAndHtmlForSameNameMovies: Map<MovieSuffix, Pair<MovieHtml, List<ActressName>>>,
        localActresses: List<Actress>,
        movieName: TauItemName
    ): Option<Pair<MovieHtml, List<ActressName>>> {
        val goodOnes = peopleAndHtmlForSameNameMovies.filter { (movieSuffix, movieThings) ->
            val ama = mutableListOf<Actress>()
            movieThings.second.forEach { actressName ->
                localActresses.firstOrNull { it.name == actressName }?.let {
                    ama.add(it)
                }
            }
            val actualMovieActresses = ama

            val movieShortcutsPresentInTheMovie = movieName.value
                .split(".")
                .drop(1)

            val actressesPresentInTheMovieName = localActresses
                .filter { it.shortcuts.any { actressShortcut -> actressShortcut in movieShortcutsPresentInTheMovie } }

            //actressesPresentInTheMovieName & actualMovieActresses
            actualMovieActresses.containsAll(actressesPresentInTheMovieName)
        }

        return if (goodOnes.isNotEmpty())
            goodOnes.values.first().toOption()
        else
            None
    }

    private suspend fun getInfosOfGoodMovies(movieSuffixes: List<MovieSuffix>): Map<MovieSuffix, Pair<MovieHtml, List<ActressName>>> {
        val result = mutableMapOf<MovieSuffix, Pair<MovieHtml, List<ActressName>>>()

        movieSuffixes.forEach { movieSuffix ->
            val movieHtml = getMovieWithSuffix(movieSuffix)
            val people: List<String> = searchPeople(movieHtml)
            result[movieSuffix] = movieHtml to people
        }

        return result
    }


    private fun searchPeople(movieHtml: MovieHtml): List<ActressName> {
        val doc = Jsoup.parse(movieHtml)
        val metas = doc.select("meta")
        val people = metas.filter { it.attr("property") == "og:video:actor" }
        val peopleNames = people.map { it.attr("content").lowercase(Locale.FRANCE) }
        return peopleNames
    }

    private fun searchTheGoodMovies(
        movieName: String,
        searchMoviesHtml: String,
    ): List<MovieSuffix> {
        val doc = Jsoup.parse(searchMoviesHtml)
        val movieCandidates = doc.select(".item-preview-video")

        val goodOnes = movieCandidates.filter { element ->
            val searchName = element.attr("itemtitle")
                .substringBefore('(')
                .removePunctuation()
                .substringBefore(" by ")
                .trim()
            searchName == movieName
        }

        val suffixes = goodOnes.mapNotNull { it.selectFirst("a")?.attr("href") }
        return suffixes
    }

    override suspend fun getMovieSubjects(movieName: TauItemName, movieHtml: MovieHtml): List<String> {
//        val shortMovieName =
//            movieName.value.substringBefore('(').removePunctuation().substringBefore(" by ")
//                .trim()
//        val searchMovieHtml = searchMoviesWithName(shortMovieName)
//        val movieSuffixes = searchTheGoodMovies(shortMovieName, searchMovieHtml)
//        val peopleAndHtmlForSameNameMovies = getInfosOfGoodMovies(movieSuffixes)

//        val movieHtml = getMovieWithSuffix(movieSuffix)
        val subjects: List<String> = searchSubjects(movieHtml)

        return subjects
    }

    private fun searchSubjects(movieHtml: String): List<String> {
        val doc = Jsoup.parse(movieHtml)
        val metas = doc.select("meta")
        val subjects = metas.filter { it.attr("property") == "og:video:tag" }
        val subjectNames = subjects.map { it.attr("content").lowercase(Locale.FRANCE) }
        return subjectNames
    }

    private suspend fun searchMoviesWithName(name: String): String =
        withContext(Dispatchers.IO) {

            moviesApi = moviesApi ?: generateApi()
            val responseBody = try {
                moviesApi!!.fetchPage(title = name)
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    println("SCRAP Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                    AppBus.lines.tryEmit("SCRAP Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                    ResponseBody.create(null, "")
                } else {
                    println("SCRAP Erreur HTTP : ${e.code()}")
                    AppBus.lines.tryEmit("SCRAP Erreur HTTP : ${e.code()}")
                    ResponseBody.create(null, "")
                }
            } catch (e: Exception) {
                println("SCRAP Erreur réseau : ${e.message}")
                AppBus.lines.tryEmit("SCRAP Erreur réseau : ${e.message}")
                ResponseBody.create(null, "")
            }

            val html = responseBody.string()
            return@withContext html
        }

    private suspend fun getMovieWithSuffix(suffix: String): String =
        withContext(Dispatchers.IO) {

            moviesApi = moviesApi ?: generateApi() ?: return@withContext ""
            val responseBody = try {
                moviesApi!!.fetchPageWithSuffix(suffix = suffix)
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    println("SCRAP Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                    AppBus.lines.tryEmit("SCRAP Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                    ResponseBody.create(null, "")
                } else {
                    println("SCRAP Erreur HTTP : ${e.code()}")
                    AppBus.lines.tryEmit("SCRAP Erreur HTTP : ${e.code()}")
                    ResponseBody.create(null, "")
                }
            } catch (e: Exception) {
                AppBus.lines.tryEmit("SCRAP Erreur réseau : ${e.message}")
                ResponseBody.create(null, "")
            }

            val html = responseBody.string()
            return@withContext html
        }

    private suspend fun generateApi(): MoviesApi = withContext(Dispatchers.IO) {
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(login, password.toCharArray())
            }
        })

        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(server, port))
        val client = OkHttpClient.Builder()
            .proxy(proxy)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .header(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    )
                    .header("Accept-Language", "fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("Referer", "https://www.google.com/")
                    .header("Cookie", "ageConfirmed=true")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.hotmovies.com/") // ← ta nouvelle base (doit se terminer par '/')
            .client(client)
            .build()
        val api = retrofit.create(MoviesApi::class.java)
        return@withContext api
    }
}

fun String.removePunctuation(): String =
    replace("\\p{Punct}".toRegex(), "")