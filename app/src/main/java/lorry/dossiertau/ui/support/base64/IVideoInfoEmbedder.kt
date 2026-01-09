package lorry.dossiertau.ui.support.base64

import android.graphics.Bitmap
import androidx.compose.ui.layout.ContentScale
import lorry.dossiertau.support.littleClasses.ColoredTag
import java.io.File

interface IVideoInfoEmbedder {
    //base64
    suspend fun appendBase64ToFile(file: File, base64Image: String, tagSuffix: Tags)
    suspend fun extractBase64FromFile(
        file: File,
        initialLookbackBytes: Int = 65536,
        maxAttempts: Int = 5,
        tagSuffix: Tags = Tags.COVER_CROPPED
    ): String?
    suspend fun removeBothBase64(file: File): Boolean
    suspend fun base64ToBitmap(base64Str: String): Bitmap?
    suspend fun bitmapToBase64(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): String
    
    //contentScale
    suspend fun appendScaleToFile(file: File, scale: ContentScale)
    suspend fun extractScaleFromFile(file: File): ContentScale?
    suspend fun removeScale(file: File): Boolean

    //flag (étiquette)
    suspend fun appendFlagToFile(file: File, flag: ColoredTag)
    suspend fun extractFlagFromFile(file: File): ColoredTag?
    suspend fun removeFlagFromFile(file: File): Boolean
    
    //memo
    suspend fun appendMemoToFile(filePath: String)
    suspend fun removeMemoFromFile(filePath: String): Boolean
}