package lorry.dossiertau.support.littleClasses

import android.R.attr.bitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable
import lorry.dossiertau.data.PreferencesAppliFavo.support.TauPathSerializer
import lorry.dossiertau.data.PreferencesAppliFavo.support.TauPictureSerializer
import java.io.ByteArrayOutputStream

@Serializable(with = TauPictureSerializer::class)
sealed class TauPicture {

    object NONE : TauPicture()

    data class Bitmap(val bitmap: android.graphics.Bitmap) : TauPicture()

    @Serializable
    data class Drawableresource(@DrawableRes val drawableresource: Int) : TauPicture()

    override fun toString(): String {
        val pictureType = when (this) {
            is Bitmap -> "Bitmap"
            is TauPicture.Drawableresource -> "Drawableresource"
            else -> "EMPTY"
        }

        return "τPicture($pictureType)"
    }

    companion object {
        fun fromBitmap(bitmap: android.graphics.Bitmap) = TauPicture.Bitmap(bitmap)
        fun fromDrawableresource(@DrawableRes drawableresource: Int) =
            Drawableresource(drawableresource)
    }

    fun toBitmap() = (this as? Bitmap)?.bitmap

    //base64
    fun toBase64(): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // On compresse le bitmap (ici en PNG pour garder la transparence)
        toBitmap()?.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Encodage en Base64
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}

fun fromBase64(base64String: String): Bitmap? {
    return try {
        // Décodage de la String en tableau d'octets
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

        // Reconstruction du Bitmap
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Bitmap.toTauPicture() = TauPicture.fromBitmap(this)
fun Int.toTauPicture() = TauPicture.fromDrawableresource(this)
