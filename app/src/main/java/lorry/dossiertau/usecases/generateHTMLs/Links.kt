package lorry.dossiertau.usecases.generateHTMLs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.usecases.generateHTMLs.repos.IDiskRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.INasRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.IWebScrappingRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.MovieHtml
import lorry.dossiertau.usecases.generateHTMLs.support.MoviesApi
import lorry.dossiertau.usecases.generateHTMLs.support.Stuff
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.net.*
import kotlin.collections.joinToString
import kotlin.collections.plus
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

typealias PictureUrl = String
typealias MovieDescription = String

class Links(
    val vm: VmLinks,
    val nasRepo: INasRepo,
    val diskRepo: IDiskRepo,
    val webScrappingRepo: IWebScrappingRepo
) {
    val login = "Pvc7NXwy6y7r33YurTuDoZ89"
    val password = "gKVRhVNy7gfjejv6qbrTVX4R"

    //    val server = "brussels.be.socks.nordhold.net"
    val server = "se.socks.nordhold.net"

    //    val server = "nl.socks.nordhold.net"
    val port = 1080

    val htmls = mutableMapOf<TauItemName, MovieHtml>()

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun generateLinks() {

        val htmls = renameFiles()

        htmls.forEach {
            val videoName = it.key
            val html = it.value

            val picture = extractPictureFrom(html)
            val description = extractDescriptionFrom(html)


        }


        println("SCRAP That's all folks!")
    }

    private fun extractDescriptionFrom(movieHtml: MovieHtml): Option<MovieDescription> {
        val doc = Jsoup.parse(movieHtml)

        val scripts = doc.select("script")
        val pictureNodes = scripts.filter { it.attr("type") == "application/ld+json" }
        if (pictureNodes.isEmpty())
            return None
        val jsons = pictureNodes.map {
            it.childNodes().first().toString()
        }
        val description = getDescriptionFromJson(jsons)
        return description

//        return description.map {
//            val doc = Jsoup.parse(it)
////            println(doc.text())  // "After Hours"
//            doc.html()  // "<p><i>After Hours</i></p>"
//        }
    }

    private fun getDescriptionFromJson(jsons: List<String>): Option<String> {

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val adapter = moshi.adapter(JsonPart::class.java)
            .lenient()
            .nullSafe()

        val parsedResult = jsons
            .map { adapter.fromJson(it) }
            .firstOrNull { it?.type == "VideoObject" }

        val description = parsedResult?.description.toOption()
        return description
    }

    private fun extractPictureFrom(movieHtml: MovieHtml): Option<PictureUrl> {

        val doc = Jsoup.parse(movieHtml)

        val links = doc.select("link")
        val pictureNode = links.filter { it.attr("name") == "thumbnail" }.firstOrNull()
        //autre image possible
//        val metas = doc.select("meta")
//        val pictureNode = metas.filter { it.attr("property") == "og:image" }

        val pictureUrl = pictureNode?.attr("content")
        return pictureUrl.toOption()
    }

    private suspend fun renameFiles(): MutableMap<TauItemName, MovieHtml> {

        val result = mutableMapOf<TauItemName, MovieHtml>()
        val fileNames = nasRepo.getVideoNames()
        (1..fileNames.size).onEach {

            println("SCRAP")
            val videoName = fileNames[it - 1]
            println("SCRAP *** FILM *** ${videoName.value}")

            val localActresses = diskRepo.getLocalActresses()
            val localSubjects = diskRepo.getLocalSubjects()

            val movieActresses = webScrappingRepo.getMovieActresses(
                name = videoName,
                localActresses = localActresses
            )

            val movieSubjects = if (!movieActresses.first.isEmpty())
                webScrappingRepo.getMovieSubjects(
                    name = videoName,
                    movieHtml = movieActresses.first
                )
            else emptyList()

            println("SCRAP movieActresses=${movieActresses.second.joinToString(",")}")
            println("SCRAP movieSubjects=${movieSubjects.joinToString(",")}")


            var newName = renameFileWithStuff(
                videoPath = videoName,
                localStuffes = localActresses,
                movieStuffes = movieActresses.second,
            )

            newName = renameFileWithStuff(
                videoPath = newName,
                localStuffes = localSubjects,
                movieStuffes = movieSubjects,
            )

            newName = newName.value.replace(" - HotMovies", "").toTauFileName()
            result[newName] = movieActresses.first

            if (newName != videoName)
                nasRepo.renameFile(videoName, newName)
        }

        return result
    }

    private fun renameFileWithStuff(
        videoPath: TauItemName,
        localStuffes: List<Stuff>,
        movieStuffes: List<String>,
    ): TauItemName {

        var result: TauItemName = videoPath
        movieStuffes.onEach { movieStuffName -> //ex: black, bandeau

            val videoShortcuts = result.value.split(".")

            localStuffes.firstOrNull { movieStuffName in it.shortcuts }?.let { correctStuff ->
//                require(correctStuff.shortcuts.isNotEmpty())
                //l'actrice n'est pas dans les shortcuts de la video
                if (videoShortcuts.none { it in correctStuff.shortcuts }) {

                    //on prend en compte anciens renommage le cas échéant
                    val newVideoPath = videoShortcuts
                        .dropLast(1)
                        .plus(correctStuff.shortcuts.first())
                        .plus(videoShortcuts.last())
                        .joinToString(".")

                    result = newVideoPath.toTauFileName()
                }
            }
        }

        return result
    }

    suspend fun fetchViaNordVPN() {
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(login, password.toCharArray())
            }
        })

//        val client = HttpClient(OkHttp) {
//            engine {
//                proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(server, port))
//                config { proxy(proxy) }
//            }
//        }
        //        val retrofit = Retrofit.Builder()
//            .baseUrl("https://www.hotmovies.com/") // ← ta nouvelle base (doit se terminer par '/')
//            .build()
//        val api = retrofit.create(MoviesApi::class.java)
//        val responseBody = api.fetchPage(title = "cheeky+and+welcoming")

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

//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://ipinfo.io/json/") // ← ta nouvelle base (doit se terminer par '/')
//            .client(client)
//            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.hotmovies.com/") // ← ta nouvelle base (doit se terminer par '/')
            .client(client)
            .build()
        val api = retrofit.create(MoviesApi::class.java)
        val responseBody = try {
            api.fetchPage(title = "cheeky+and+welcoming")
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
        println("SCRAP KTOR html=$html")

        ///////////////////////////////////////////////////////////////////////////
        val responseBody2 = try {
            api.fetchPage2()
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

        val html2 = responseBody2.string()
        println("SCRAP KTOR html=$html2")


//        val textResp0 = client.get("https://ipinfo.io/json")  // Test IP d'abord
//        println("KTOR IP via Nord SOCKS5: ${textResp0.bodyAsText().replace("\n", "")}")
//
//        // Texte
//        val textResp: HttpResponse = client.get("https://stackoverflow.com/questions/71980361/how-to-get-a-image-png-with-ktor-client-get-request")
//        val text = textResp.bodyAsText()
//        println("KTOR text: $text")
//
//        // Image (faible, bytes)
//        val imageBytes: ByteArray =
//            client.get("https://www.lacremedugaming.fr/wp-content/uploads/creme-gaming/2025/12/fallout-saison-2-date-et-heure-de-sortie-episode-4.jpg")
//                .readBytes()
//        val picture = imageBytes.toBitmap()
//        println("KTOR image: ${imageBytes.toString()}")

        //IMPORTANT
//        client.close()
    }
}

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun ByteArray.toBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size)
}

@JsonClass(generateAdapter = true)
data class JsonPart(
    @Json(name = "@type") val type: String,
    val description: String?
)