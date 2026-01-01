package lorry.dossiertau.usecases.generateHTMLs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.*

class Links(
    val vm: VmLinks
) {
    val login = "Pvc7NXwy6y7r33YurTuDoZ89"
    val password = "gKVRhVNy7gfjejv6qbrTVX4R"
    val server = "nl.socks.nordhold.net"
    val port = 1080

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun generateLinks() {
        scope.launch {
            fetchViaNordVPN()
        }
    }

    suspend fun fetchViaNordVPN() {
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(login, password.toCharArray())
            }
        })

        val client = HttpClient(OkHttp) {
            engine {
                proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(server, port))
                config { proxy(proxy) }
            }
        }

        val textResp0 = client.get("https://ipinfo.io/json")  // Test IP d'abord
        println("KTOR IP via Nord SOCKS5: ${textResp0.bodyAsText().replace("\n", "")}")

        // Texte
        val textResp: HttpResponse = client.get("https://stackoverflow.com/questions/71980361/how-to-get-a-image-png-with-ktor-client-get-request")
        val text = textResp.bodyAsText()
        println("KTOR text: $text")

        // Image (faible, bytes)
        val imageBytes: ByteArray =
            client.get("https://www.lacremedugaming.fr/wp-content/uploads/creme-gaming/2025/12/fallout-saison-2-date-et-heure-de-sortie-episode-4.jpg")
                .readBytes()
        val picture = imageBytes.toBitmap()
        println("KTOR image: ${imageBytes.toString()}")

        client.close()
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