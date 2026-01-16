package lorry.folder.items.dossiersigma.external.base64

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.base64.IBase64DataSource
import lorry.dossiertau.support.littleClasses.TauPath

class Base64DataSource: IBase64DataSource {

    override suspend fun extractImageFromHtml(html: TauPath): Bitmap? {

        val htmlFile = html.toFile().getOrNull() ?: return null
        if (!withContext(Dispatchers.IO) { htmlFile.exists() == true}) return null

        val htmlContent = withContext(Dispatchers.IO) { htmlFile.readText() }

        // Regex pour trouver le contenu de src="data:image/...;base64,..."
        val regex = Regex("""<img\s+[^>]*src\s*=\s*"data:image/[^;]+;base64,([^"]+)"""")
        val match = regex.find(htmlContent) ?: return null

        val base64Image = match.groupValues[1]
        return try {
            withContext(Dispatchers.Default) {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }

        } catch (e: Exception) {
            println("Erreur lors du décodage de l'image : ${e.message}")
            null
        }
    }

    override suspend fun extractBase64FromHtml(html: TauPath): String? {

        val htmlFile = html.toFile().getOrNull() ?: return null
        if (!withContext(Dispatchers.IO) { htmlFile.exists() }) return null

        val htmlContent = withContext(Dispatchers.IO) { htmlFile.readText() }

        // Regex pour trouver le contenu de src="data:image/...;base64,..."
        val regex = Regex("""<img\s+[^>]*src\s*=\s*"data:image/[^;]+;base64,([^"]+)"""")
        val match = regex.find(htmlContent) ?: return null

        val base64Image = match.groupValues[1]
        return base64Image
    }
}
