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
import data.ftp.IFtpDS
import lorry.dossiertau.ShortcutMakingEndMessage
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.ui.AppBus
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.ActressName
import lorry.dossiertau.usecases.generateHTMLs.support.Subject

typealias PictureUrl = String
typealias MovieDescription = String

class Links(
    val vm: VmLinks,
    val nasRepo: INasRepo,
    val diskRepo: IDiskRepo,
    val webScrappingRepo: IWebScrappingRepo,
    val ftpDS: IFtpDS
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

        println("SCRAP DEUXIEME PHASE: enregistrements metadatas")
        AppBus.lines.tryEmit("SCRAP DEUXIEME PHASE: enregistrements metadatas")

        htmls.onEachIndexed { index, it ->
            println("SCRAP")
            AppBus.lines.tryEmit("SCRAP")
            println("SCRAP ⯈⯈⯈ ${it.key.value} ...")
            AppBus.summary.tryEmit("II$index/${htmls.size}\uD83D\uDCBE")
            AppBus.lines.tryEmit("SCRAP ⯈⯈⯈ ${it.key.value} ...")
            val videoName = it.key
            val html = it.value

            /////////////////
            // description //
            /////////////////
            saveDescription(html = html.html, videoName = videoName)
            println("SCRAP description enregistrée")
            AppBus.lines.tryEmit("SCRAP description enregistrée")

            ///////////
            // image //
            ///////////
            savePicture(html = html.html, videoName = videoName)
            println("SCRAP image enregistrée")
            AppBus.lines.tryEmit("SCRAP image enregistrée")
        }

        //////////////////////////////////
        // suppression de tous les html //
        //////////////////////////////////
        println("SCRAP")
        AppBus.lines.tryEmit("SCRAP")
        println("SCRAP TROISIEME PHASE: suppression des htmls ...")
        AppBus.lines.tryEmit("SCRAP TROISIEME PHASE: suppression des htmls ...")
        AppBus.summary.tryEmit("III\uD83D\uDDF4\uD83D\uDDF4\uD83D\uDDF4\uD83D\uDD71")
        diskRepo.deleteAllHtmlsIn("/storage/emulated/0/Movies/sexe/filles".toTauPath())
        diskRepo.deleteAllHtmlsIn("/storage/emulated/0/Movies/sexe/fantasmes".toTauPath())
        println("SCRAP suppression effectués")
        AppBus.lines.tryEmit("SCRAP suppression effectués")

        println("SCRAP")
        AppBus.lines.tryEmit("SCRAP")
        println("SCRAP QUATRIEME PHASE: création des Htmls")
        AppBus.lines.tryEmit("SCRAP QUATRIEME PHASE: création des Htmls")
        println("SCRAP")
        AppBus.lines.tryEmit("SCRAP")
        htmls.onEachIndexed { index, it ->

            ////////////////////////////////
            // création des nouveaux html //
            ////////////////////////////////
            println("SCRAP ⯈ création des htmls de ${it.key.value}")
            AppBus.lines.tryEmit("SCRAP ⯈ création des htmls de ${it.key.value}")
            AppBus.summary.tryEmit("IV$index/${htmls.size}\uD83D\uDC27")

            createFillesHtmls(
                annexesNasPath = "/annexes",
                videoFile = it,
            )

            createSubjectsHtmls(
                annexesNasPath = "/annexes",
                videoFile = it,
            )

            println("SCRAP ... fichier traité")
            AppBus.lines.tryEmit("SCRAP ... fichier traité")
        }

        println("SCRAP That's all folks!")
        AppBus.lines.tryEmit("SCRAP That's all folks!")
        AppBus.lines.tryEmit(ShortcutMakingEndMessage)
    }

    private suspend fun createSubjectsHtmls(
        annexesNasPath: String,
        videoFile: Map.Entry<TauItemName, GrabbedFromPhase1>
    ) {
        if (videoFile.key.value.lowercase().contains("gang bang vol"))
            println("ok")

        val picture64 = ftpDS.readJpgFromFtpAsBase64(videoFile.key)

        println("SCRAP ... contenu image récupéré pour création HTML: ${picture64?.take(8)}")
        AppBus.lines.tryEmit("SCRAP ... contenu image récupéré pour création HTML: ${picture64?.take(8)}")

        val subjectPaths: Map<Subject, TauItemName> = getLocalSubjects()

        val htmlContent = createHtmlContent(
            annexes = annexesNasPath,
            videoName = videoFile.key,
            picture64 = picture64,
        )

        //les fantasmes de "Fantasmes"
        subjectPaths.onEach { subjectInSubjectsFolder ->
            //les sujets du film
            videoFile.value.subjects.onEach { subjectNameInVideo ->
                if (subjectNameInVideo == subjectInSubjectsFolder.key){
                    //le sujet dans "Fantasmes" subjectInSubjectsFolder.key
                    //correspond à un des sujets du film

                    createSubjectHtmlFile(
                        htmlContent = htmlContent,
                        folder = subjectInSubjectsFolder.value,
                        videoName = videoFile.key
                    )
                }
            }
        }
    }

    private suspend fun createFillesHtmls(
        annexesNasPath: String,
        videoFile: Map.Entry<TauItemName, GrabbedFromPhase1>
    ) {
        val picture64 = ftpDS.readJpgFromFtpAsBase64(videoFile.key)
        val description = ftpDS.readDescriptionFromFtpAsBase64(videoFile.key)

        println("SCRAP ... contenu image récupéré pour création HTML: ${picture64?.take(8)}")
        AppBus.lines.tryEmit("SCRAP ... contenu image récupéré pour création HTML: ${picture64?.take(8)}")
        println("SCRAP ... contenu description récupéré pour création HTML: $description")
        AppBus.lines.tryEmit("SCRAP ... contenu description récupéré pour création HTML: $description")

        val actressPaths: Map<Actress, TauItemName> = getLocalActresses()

        val htmlContent = createHtmlContent(
            annexes = annexesNasPath,
            videoName = videoFile.key,
            picture64 = picture64,
            description = description
        )

        //les actrices de "Filles"
        actressPaths.onEach { actressAndMovieInFilles ->
            //les actrices du film
            videoFile.value.actresses.onEach { actressNameInVideo ->
                if (actressNameInVideo.lowercase() in actressAndMovieInFilles.key.shortcuts){
                    //la fille dans "Filles" actressAndMovieInFilles.key
                    //correspond à une des actrices du film

                    createFillesHtmlFile(
                        htmlContent = htmlContent,
                        folder = actressAndMovieInFilles.value,
                        videoName = videoFile.key
                    )
                }
            }
        }
    }

    private fun createFillesHtmlFile(
        htmlContent: MovieHtml,
        folder: TauItemName,
        videoName: TauItemName
    ) {
        val fullPath = "/storage/emulated/0/Movies/sexe/filles/${folder.value}/${videoName.value}"
            .substringBeforeLast(".") + ".html"
        fullPath.toTauPath().toFile().getOrNull()?.let{
            it.createNewFile()
            it.writeText(htmlContent, Charsets.UTF_8)
        }
    }

    private fun createSubjectHtmlFile(
        htmlContent: MovieHtml,
        folder: TauItemName,
        videoName: TauItemName
    ) {
        val fullPath = "/storage/emulated/0/Movies/sexe/fantasmes/${folder.value}/${videoName.value}"
            .substringBeforeLast(".") + ".html"
        fullPath.toTauPath().toFile().getOrNull()?.let{
            it.createNewFile()
            it.writeText(htmlContent, Charsets.UTF_8)
        }
    }

    private fun createHtmlContent(
        annexes: String,
        videoName: TauItemName,
        picture64: String? = null,
        description: String? = null
    ): MovieHtml {
        var nas = "smb://olivier:37-2lematin@192.168.1.20/videos/${videoName.value}?player=vlc"

        val imageSection = picture64?.let {
            """<img src="data:image/jpeg;base64,$it" alt="cover" style="max-width:100%;height:auto;"/><br>"""
        } ?: ""

        val text = """<!DOCTYPE html>
                                 <html lang="fr">
                                 <head>
                                     <meta charset="UTF-8">
                                     <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                     <title>Redirection automatique</title>
                                 </head>
                                 <body>
                                $imageSection
                                 <a id="autoClickLink" href="myapp://playvideo/kiwi?video=$nas">Lien automatique</a>

                                 <script>
                                     window.onload = function() {
                                         // Récupère le lien par son identifiant et déclenche le clic
                                         document.getElementById("autoClickLink").click();
                                     };
                                 </script>

                                 </body>
                                 </html>"""

        return text

    }

    private suspend fun getLocalActresses(): Map<Actress, TauItemName> {

        val fillesPaths = withContext(Dispatchers.IO) {"/storage/emulated/0/Movies/sexe/filles".toTauPath().toFile()
            .getOrNull()
            ?.listFiles()?.filter { it.isDirectory }
        }

        val result = mutableMapOf<Actress, TauItemName>()

        fillesPaths?.onEach { file ->
            val actress = file.name
                .split(",")
                .let { items ->
                    if (items.size == 1)
                        Actress(
                            name = items.first(),
                            shortcuts = listOf(items.first())
                        )
                    else
                        Actress(
                            name = items[1],
                            shortcuts = items
                        )
                }

            result[actress] = TauItemName(file.name)
        }

        return result
    }

    private suspend fun getLocalSubjects(): Map<Subject, TauItemName> {

        val subjectsPaths = withContext(Dispatchers.IO) {
            "/storage/emulated/0/Movies/sexe/fantasmes".toTauPath().toFile()
                .getOrNull()
                ?.listFiles()?.filter { it.isDirectory }
        }

        val result = mutableMapOf<Subject, TauItemName>()

        subjectsPaths?.onEach { file ->
            val subject = file.name
                .split(",")
                .let { items ->
                    if (items.size == 1)
                        Subject(
                            name = items.first(),
                            shortcuts = listOf(items.first())
                        )
                    else Subject(
                        name = items.first(),
                        shortcuts = items
                    )
                }

            result[subject] = TauItemName(file.name)
        }

        return result
    }

    private suspend fun savePicture(
        html: MovieHtml,
        videoName: TauItemName
    ) {
        val picture = extractPictureFrom(html)
        println("SCRAP ◕ image: ${if (picture.isSome()) "présente" else "absente"}")
        AppBus.lines.tryEmit("SCRAP ◕ image: ${if (picture.isSome()) "présente" else "absente"}")

        val pictureOk = picture.fold(ifSome = {
            ftpDS.createPictureFileInAnnexes(videoName, it as String)
        }, ifEmpty = { false })
        println("SCRAP ⏺ enregistrement image: ${if (pictureOk) "ok" else "problème"}")
        AppBus.lines.tryEmit("SCRAP ⏺ enregistrement image: ${if (pictureOk) "ok" else "problème"}")
    }

    private suspend fun saveDescription(html: MovieHtml, videoName: TauItemName) {
        val description = extractDescriptionFrom(html)
        println("SCRAP ◔ description: ${description.getOrNull()?.length ?: 0} caractères")
        AppBus.lines.tryEmit("SCRAP ◔ description: ${description.getOrNull()?.length ?: 0} caractères")

        val descriptionOk = description.fold(ifSome = {
            ftpDS.createDescriptionFileInAnnexes(videoName, it)
        }, ifEmpty = { false })
        println("SCRAP ◑ enregistrement description: ${if (descriptionOk) "ok" else "problème"}")
        AppBus.lines.tryEmit("SCRAP ◑ enregistrement description: ${if (descriptionOk) "ok" else "problème"}")
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

        return description.map {
            val doc = Jsoup.parse(it)
            doc.text()  // "After Hours"
//            doc.html()  // "<p><i>After Hours</i></p>"
        }

        return description
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

//        val links = doc.select("link")
//        val pictureNode = links.filter { it.attr("name") == "thumbnail" }.firstOrNull()
        //autre image possible
        val metas = doc.select("meta")
        val pictureNode = metas.filter { it.attr("property") == "og:image" }.firstOrNull()

        val pictureUrl = pictureNode?.attr("content")
        return pictureUrl.toOption()
    }

    private suspend fun renameFiles(): MutableMap<TauItemName, GrabbedFromPhase1> {

        val result = mutableMapOf<TauItemName, GrabbedFromPhase1>()
        val fileNames = nasRepo.getVideoNames()

        println("SCRAP PREMIERE PHASE: renommage de tous les fichiers")
        AppBus.lines.tryEmit("SCRAP PREMIERE PHASE: renommage de tous les fichiers")

        (1..fileNames.size).onEach {

            println("SCRAP")
            AppBus.lines.tryEmit("SCRAP")
            AppBus.summary.tryEmit("I$it/${fileNames.size}\uD83D\uDCEA")
            val videoName = fileNames[it - 1]
            println("SCRAP ⯈⯈⯈ ${videoName.value}")
            AppBus.lines.tryEmit("SCRAP ⯈⯈⯈ ${videoName.value}")

            if (videoName.value.lowercase().contains("gang bang vol"))
                println("ok")

            val localActresses = diskRepo.getLocalActresses()
            val localSubjects = diskRepo.getLocalSubjects()

            val movieActresses = webScrappingRepo.getMovieActresses(
                name = videoName,
                localActresses = localActresses
            )

            val movieSubjects = if (!movieActresses.first.isEmpty())
                webScrappingRepo.getMovieSubjects(
                    name = videoName,
                    movieHtml = movieActresses.first,
                    localSubjects = localSubjects
                )
            else emptyList()

            println("SCRAP ⯈ ${movieActresses.second.joinToString(",")}")
            AppBus.lines.tryEmit("SCRAP ⯈ ${movieActresses.second.joinToString(",")}")

            println("SCRAP ⯈ ${movieSubjects.joinToString(",")}")
            AppBus.lines.tryEmit("SCRAP ⯈ ${movieSubjects.joinToString(",")}")


            var newName = renameFileWithStuff(
                videoPath = videoName,
                localStuffes = localActresses,
                movieStuffes = movieActresses.second,
            )

            newName = renameFileWithStuff(
                videoPath = newName,
                localStuffes = localSubjects,
                movieStuffes = movieSubjects.map{ it.name },
            )

            newName = newName.value.replace(" - HotMovies", "").toTauFileName()
            result[newName] = GrabbedFromPhase1(
                html = movieActresses.first,
                actresses = movieActresses.second,
                subjects = movieSubjects
            )

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
        println("SCRAP KTOR html=$html")
        AppBus.lines.tryEmit("SCRAP KTOR html=$html")

        ///////////////////////////////////////////////////////////////////////////
        val responseBody2 = try {
            api.fetchPage2()
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

        val html2 = responseBody2.string()
        println("SCRAP KTOR html=$html2")
        AppBus.lines.tryEmit("SCRAP KTOR html=$html2")


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

typealias SubjectName = String

data class GrabbedFromPhase1(
    val html: MovieHtml,
    val actresses: List<ActressName>,
    val subjects: List<Subject>
)