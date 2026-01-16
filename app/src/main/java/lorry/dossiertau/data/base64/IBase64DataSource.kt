package lorry.dossiertau.data.base64

import android.graphics.Bitmap
import lorry.dossiertau.support.littleClasses.TauPath

interface IBase64DataSource {
    
    suspend fun extractImageFromHtml(html: TauPath): Bitmap?
    suspend fun extractBase64FromHtml(html: TauPath): String?
}