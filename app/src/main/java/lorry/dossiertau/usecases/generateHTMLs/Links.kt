package lorry.dossiertau.usecases.generateHTMLs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import data.ftp.IFtpDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import lorry.dossiertau.ShortcutMakingEndMessage
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauFileName
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.ui.AppBus
import lorry.dossiertau.usecases.generateHTMLs.repos.DiskRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.IDiskRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.INasRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.IWebScrappingRepo
import lorry.dossiertau.usecases.generateHTMLs.repos.MovieHtml
import lorry.dossiertau.usecases.generateHTMLs.support.Actress
import lorry.dossiertau.usecases.generateHTMLs.support.ActressName
import lorry.dossiertau.usecases.generateHTMLs.support.MoviesApi
import lorry.dossiertau.usecases.generateHTMLs.support.Stuff
import lorry.dossiertau.usecases.generateHTMLs.support.Subject
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import kotlin.collections.map

typealias PictureUrl = String
typealias MovieDescription = String

/**
 * USAGE
 * ```
 * fun resetStuff() à appeler au besoin
 * readyFlow<Boolean>
 * val actressesAndItemNameFlow: flow de List<Pair<Actress, TauItemName>>
 * val subjectsAndItemNamesFlow: flow de List<Pair<Subject, Set<TauItemName>>>
 * val actressesFlow: flow de List<Actress>
 * val subjectsFlow: flow de List<Subject>
 * ```
 */
object LocalActressesAndSubjects {
    private val diskRepo = DiskRepo()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _actressFNFlow = MutableSharedFlow<List<Pair<Actress, TauItemName>>?>(replay = 1)
    private val _subjectFNFlow =
        MutableSharedFlow<List<Pair<Subject, Set<TauItemName>>>?>(replay = 1)

    val getStuffFlow = combine(_actressFNFlow, _subjectFNFlow) { a, s ->
        a to s
    }.shareIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    val actressesAndItemNameFlow = getStuffFlow.map { it.first ?: emptyList() }
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    val subjectAndItemNamesFlow = getStuffFlow.map { it.second ?: emptyList() }
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    val actressesFlow = getStuffFlow.map { stuff ->
        stuff.first?.map { actress ->
            actress.first
        } ?: emptyList()
    }
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 1
        )


    val subjectsFlow = getStuffFlow.map { stuff ->
        stuff.second?.map { subject ->
            subject.first
        } ?: emptyList()
    }
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    val readyFlow = getStuffFlow.map { stuff ->
        stuff.first != null && stuff.second != null
    }
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    init {
        resetStuff()
    }

    fun resetStuff() {
        scope.launch {
            val localActressesFN = diskRepo.getLocalActressesAndFileNames()
            val localSubjectsFN = diskRepo.getLocalSubjectsAndFileNames()
            _actressFNFlow.tryEmit(localActressesFN)
            _subjectFNFlow.tryEmit(localSubjectsFN)
        }
    }
}

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

        LocalActressesAndSubjects.resetStuff()
        LocalActressesAndSubjects.readyFlow
            .takeWhile { !it }
            .collect()

        //////////////////////////////////
        // suppression de tous les html //
        //////////////////////////////////
        diskRepo.deleteAllHtmlsIn(FOLDER_FILLES)
        diskRepo.deleteAllHtmlsIn(FOLDER_SUBJECTS)

        ///////////////////////
        // boucle principale //
        ///////////////////////
        val _htmls: Set<HtmlPacket> = getHtmlPackets()
        log("${_htmls.size} htmls à traiter")
        _htmls.onEachIndexed { index, packet ->

            log("")
            log("${index + 1}/${_htmls.size} (${packet.videoName.value}) en cours")
            logSummary("${index + 1}/${_htmls.size}")
            val videoName = packet.videoName
            val movieActresses = packet.actresses
            val movieSubjects = packet.subjects
            val html = packet.html

            ///////////////
            // renommage //
            ///////////////
            log("renommage ...")
            val newName = computeNewName(videoName, movieActresses, movieSubjects)
            renameFile(videoName, newName)

            /////////////////////////////////
            // écriture du méta sur le NAS //
            /////////////////////////////////
            //description
            log("stockage description ...")
            saveDescription(html = html, videoName = videoName)
            //image
            log("stockage image ...")
            savePicture(html = html, videoName = videoName)

            //////////////////////////////////
            // création des raccourcis html //
            //////////////////////////////////
            log("création des HTML (filles) ...")
            createFillesHtmls(
                annexesNasPath = "/annexes",
                videoName = videoName,
                actresses = movieActresses
            )

            log("création des HTML (fantasmes) ...")
            createSubjectsHtmls(
                annexesNasPath = "/annexes",
                videoName = videoName,
                subjects = movieSubjects
            )
        }

        log(ShortcutMakingEndMessage)
    }

    private suspend fun renameFile(videoName: TauItemName, newName: TauItemName) {
        if (newName != videoName)
            nasRepo.renameFile(videoName, newName)
    }

    private suspend fun computeNewName(
        videoName: TauItemName,
        movieActresses: List<ActressName>,
        movieSubjects: List<Subject>
    ): TauItemName {

        var newName = renameFileWithStuff(
            videoPath = videoName,
            localStuffes = LocalActressesAndSubjects.actressesFlow.first(),
            movieStuffes = movieActresses,
        )

        newName = renameFileWithStuff(
            videoPath = newName,
            localStuffes = LocalActressesAndSubjects.subjectsFlow.first(),
            movieStuffes = movieSubjects.map { it.name },
        )

        newName = newName.value.replace(" - HotMovies", "").toTauFileName()
        return newName
    }

    suspend fun  getHtmlPackets(): Set<HtmlPacket> {

        val fileNames = nasRepo.getVideoNames()
        val result = mutableSetOf<HtmlPacket>()

        fileNames.onEachIndexed { index, videoName ->

            val movieActresses = webScrappingRepo.getMovieActresses(
                name = videoName,
                localActresses = LocalActressesAndSubjects.actressesFlow.first()
            )

            val movieSubjects = if (!movieActresses.first.isEmpty())
                webScrappingRepo.getMovieSubjects(
                    name = videoName,
                    movieHtml = movieActresses.first,
                    localSubjects = LocalActressesAndSubjects.subjectsFlow.first()
                )
            else emptyList()

            result.add(
                HtmlPacket(
                    videoName = videoName,
                    html = movieActresses.first,
                    actresses = movieActresses.second,
                    subjects = movieSubjects
                )
            )
        }

        return result
    }

    private suspend fun createSubjectsHtmls(
        annexesNasPath: String,
        videoName: TauItemName,
        subjects: List<Subject>,
    ) {
        if (videoName.value.lowercase().contains("gang bang vol"))
            println("ok")

        val picture64 = ftpDS.readJpgFromFtpAsBase64(videoName)

        println("SCRAP ... contenu image récupéré pour création HTML: ${picture64?.take(8)}")
        AppBus.lines.tryEmit(
            "SCRAP ... contenu image récupéré pour création HTML: ${
                picture64?.take(
                    8
                )
            }"
        )

        val subjectPaths = LocalActressesAndSubjects.subjectAndItemNamesFlow.first()

        val htmlContent = createHtmlContent(
            annexes = annexesNasPath,
            videoName = videoName,
            picture64 = picture64,
        )

        //les fantasmes de "Fantasmes"
        subjectPaths.onEach { subjectInSubjectsFolder ->
            //les sujets du film
            subjects.onEach { subjectNameInVideo ->
                if (subjectNameInVideo == subjectInSubjectsFolder.first) {
                    //le sujet dans "Fantasmes" subjectInSubjectsFolder.key
                    //correspond à un des sujets du film

                    subjectInSubjectsFolder.second.onEach {
                        createSubjectHtmlFile(
                            htmlContent = htmlContent,
                            folder = it,
                            videoName = videoName
                        )
                    }
                }
            }
        }
    }

    private suspend fun createFillesHtmls(
        annexesNasPath: String,
        videoName: TauItemName,
        actresses: List<ActressName>,
        ) {
        val picture64 = ftpDS.readJpgFromFtpAsBase64(videoName)
        val description = ftpDS.readDescriptionFromFtpAsBase64(videoName)

        println("SCRAP ... contenu image récupéré pour création HTML: ${picture64?.take(8)}")
        AppBus.lines.tryEmit(
            "SCRAP ... contenu image récupéré pour création HTML: ${
                picture64?.take(
                    8
                )
            }"
        )
        println("SCRAP ... contenu description récupéré pour création HTML: $description")
        AppBus.lines.tryEmit("SCRAP ... contenu description récupéré pour création HTML: $description")

        val actressPaths = LocalActressesAndSubjects.actressesAndItemNameFlow.first()

        val htmlContent = createHtmlContent(
            annexes = annexesNasPath,
            videoName = videoName,
            picture64 = picture64,
            description = description
        )

        //les actrices de "Filles"
        actressPaths.onEach { actressAndMovieInFilles ->
            //les actrices du film
            actresses.onEach { actressNameInVideo ->
                if (actressNameInVideo.lowercase() in actressAndMovieInFilles.first.shortcuts) {
                    //la fille dans "Filles" actressAndMovieInFilles.key
                    //correspond à une des actrices du film

                    createFillesHtmlFile(
                        htmlContent = htmlContent,
                        folder = actressAndMovieInFilles.second,
                        videoName = videoName
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
        fullPath.toTauPath().toFile().getOrNull()?.let {
            it.createNewFile()
            it.writeText(htmlContent, Charsets.UTF_8)
        }
    }

    private fun createSubjectHtmlFile(
        htmlContent: MovieHtml,
        folder: TauItemName,
        videoName: TauItemName
    ) {
        val fullPath =
            "/storage/emulated/0/Movies/sexe/fantasmes/${folder.value}/${videoName.value}"
                .substringBeforeLast(".") + ".html"
        fullPath.toTauPath().toFile().getOrNull()?.let {
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
}

suspend fun logSummary(text: String) {
    AppBus.summary.emit(text)
}

suspend fun log(text: String) {
    println(text)
    AppBus.lines.emit(text)
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

data class HtmlPacket(
    val videoName: TauItemName,
    val html: MovieHtml,
    val actresses: List<ActressName>,
    val subjects: List<Subject>
)

val FOLDER_FILLES = "/storage/emulated/0/Movies/sexe/filles".toTauPath()
val FOLDER_SUBJECTS = "/storage/emulated/0/Movies/sexe/fantasmes".toTauPath()