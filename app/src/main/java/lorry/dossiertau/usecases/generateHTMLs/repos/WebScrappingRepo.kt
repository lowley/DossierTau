package lorry.dossiertau.usecases.generateHTMLs.repos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.support.littleClasses.TauItemName
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

class WebScrappingRepo: IWebScrappingRepo {

    val login = "Pvc7NXwy6y7r33YurTuDoZ89"
    val password = "gKVRhVNy7gfjejv6qbrTVX4R"
    val server = "se.socks.nordhold.net"
    val port = 1080

    var moviesApi: MoviesApi? = null

    override suspend fun getMovieActresses(movieName: TauItemName): List<String>{

        val shortMovieName = movieName.value.substringBefore('(').removePunctuation().substringBefore(" by ").trim()
        val searchMovieHtml = searchMoviesWithName(shortMovieName)
        val movieSuffix = searchSuffix(shortMovieName, searchMovieHtml) ?: return emptyList()
        val movieHtml = getMovieWithSuffix(movieSuffix)
        val people: List<String> = searchPeople(movieHtml)

        return people
    }


    private fun searchPeople(movieHtml: String): List<String> {
        val doc = Jsoup.parse(movieHtml)
        val metas = doc.select("meta")
        val people = metas.filter { it.attr("property") == "og:video:actor" }
        val peopleNames = people.map { it.attr("content").lowercase(Locale.FRANCE) }
        return peopleNames
    }

    private fun searchSuffix(
        movieName: String,
        searchMovieHtml: String
    ): String? {
        val doc = Jsoup.parse(searchMovieHtml)
        val movieCandidates = doc.select(".item-preview-video")
        println("SCRAP jsoup: ${movieCandidates.size} movie candidates")

        val goodOne = movieCandidates.firstOrNull { element ->
            val searchName = element.attr("itemtitle")
                .substringBefore('(')
                .removePunctuation()
                .substringBefore(" by ")
                .trim()
            searchName == movieName
        }

        val suffix = goodOne?.selectFirst("a")?.attr("href") ?: null
        return suffix
    }

    override suspend fun getMovieSubjects(movieName: TauItemName): List<String>{
        val shortMovieName = movieName.value.substringBefore('(').removePunctuation().substringBefore(" by ").trim()
        val searchMovieHtml = searchMoviesWithName(shortMovieName)
        val movieSuffix = searchSuffix(shortMovieName, searchMovieHtml) ?: return emptyList()
        val movieHtml = getMovieWithSuffix(movieSuffix)
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

    private suspend fun searchMoviesWithName(name: String): String = withContext(Dispatchers.IO) {

        moviesApi = moviesApi ?: generateApi() ?: return@withContext ""
        val responseBody = try {
            moviesApi!!.fetchPage(title = name)
        } catch (e: HttpException) {
            if (e.code() == 403) {
                println("SCRAP Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                ResponseBody.create(null, "")
            } else {
                println("SCRAP Erreur HTTP : ${e.code()}")
                ResponseBody.create(null, "")
            }
        } catch (e: Exception) {
            println("SCRAP Erreur réseau : ${e.message}")
            ResponseBody.create(null, "")
        }

        val html = responseBody.string()
        return@withContext html
    }

    private suspend fun getMovieWithSuffix(suffix: String): String = withContext(Dispatchers.IO) {

        moviesApi = moviesApi ?: generateApi() ?: return@withContext ""
        val responseBody = try {
            moviesApi!!.fetchPageWithSuffix(suffix = suffix)
        } catch (e: HttpException) {
            if (e.code() == 403) {
                println("SCRAP Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                ResponseBody.create(null, "")
            } else {
                println("SCRAP Erreur HTTP : ${e.code()}")
                ResponseBody.create(null, "")
            }
        } catch (e: Exception) {
            println("SCRAP Erreur réseau : ${e.message}")
            ResponseBody.create(null, "")
        }

        val html = responseBody.string()
        return@withContext html
    }

    private suspend fun generateApi(): MoviesApi = withContext(Dispatchers.IO){
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