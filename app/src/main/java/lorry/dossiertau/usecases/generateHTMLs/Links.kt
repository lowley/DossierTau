package lorry.dossiertau.usecases.generateHTMLs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.usecases.generateHTMLs.repos.IDiskRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.INasRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.IWebScrappingRepo
import lorry.dossiertau.usecases.generateHTMLs.support.MoviesApi
import lorry.dossiertau.usecases.generateHTMLs.support.Stuff
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.net.*
import kotlin.collections.joinToString
import kotlin.collections.plus

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

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun generateLinks() {

        val fileNames = nasRepo.getVideoNames()
        (1..fileNames.size).onEach {

            val videoName = fileNames[it - 1]

            val localActresses = diskRepo.getLocalActresses()
            val localSubjects = diskRepo.getLocalSubjects()

            val movieActresses = webScrappingRepo.getMovieActresses(name = videoName)
            val movieSubjects = webScrappingRepo.getMovieSubjects(name = videoName)

            var newName = renameFileWithStuff(
                videoPath = videoName,
                localStuffes = localActresses,
                movieStuffes = movieActresses,
            )

            newName = renameFileWithStuff(
                videoPath = newName,
                localStuffes = localSubjects,
                movieStuffes = movieSubjects,
            )

            if (newName != videoName)
                nasRepo.renameFile(videoName, newName)
        }

//        scope.launch(Dispatchers.IO) {
//            fetchViaNordVPN()
//        }
    }

    private fun renameFileWithStuff(
        videoPath: TauItemName,
        localStuffes: List<Stuff>,
        movieStuffes: List<Stuff>,
    ): TauItemName {

        var result: TauItemName = videoPath
        movieStuffes.onEach { movieStuff ->

            val videoShortcuts = result.value.split(".")

            //utilise [[égalité des Actress]]
            if (movieStuff in localStuffes) {
                //l'actrice n'est pas dans les shortcuts de la video
                if (movieStuff.shortcuts.none { shortcut ->
                        shortcut in videoShortcuts }) {

                    //on prend en compte anciens renommage le cas échéant
                    val newVideoPath = videoShortcuts
                        .dropLast(1)
                        .plus(movieStuff.shortcuts.first())
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
                println("Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                ResponseBody.create(null, "")
            } else {
                println("Erreur HTTP : ${e.code()}")
                ResponseBody.create(null, "")
            }
        } catch (e: Exception) {
            println("Erreur réseau : ${e.message}")
            ResponseBody.create(null, "")
        }

        val html = responseBody.string()
        println("KTOR html=$html")

        ///////////////////////////////////////////////////////////////////////////
        val responseBody2 = try {
            api.fetchPage2()
        } catch (e: HttpException) {
            if (e.code() == 403) {
                println("Accès refusé : Le site bloque peut-être votre Proxy ou nécessite des headers plus complets.")
                ResponseBody.create(null, "")
            } else {
                println("Erreur HTTP : ${e.code()}")
                ResponseBody.create(null, "")
            }
        } catch (e: Exception) {
            println("Erreur réseau : ${e.message}")
            ResponseBody.create(null, "")
        }

        val html2 = responseBody2.string()
        println("KTOR html=$html2")


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