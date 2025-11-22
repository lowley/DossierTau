package lorry.dossiertau.support.littleClasses

import android.graphics.Bitmap
import androidx.annotation.DrawableRes

sealed class TauPicture {

    object NONE : TauPicture()
    data class Bitmap(val bitmap: android.graphics.Bitmap) : TauPicture()
    data class Drawableresource(@DrawableRes val drawableresource: Int) : TauPicture()

    companion object {
        fun fromBitmap(bitmap: android.graphics.Bitmap) = TauPicture.Bitmap(bitmap)
        fun fromDrawableresource(@DrawableRes drawableresource: Int) =
            Drawableresource(drawableresource)
    }

    fun toBitmap() = (this as? Bitmap)?.bitmap

    //base64
    fun toBase64(): String {
        throw NotImplementedError()
    }

    fun fromBase64(base64: String) {
        throw NotImplementedError()
    }


    override fun toString(): String {
        val pictureType = when (this) {
            is Bitmap -> "Bitmap"
            is Drawableresource -> "Drawableresource"
            else -> "EMPTY"
        }

        return "τPicture($pictureType)"
    }
}

fun Bitmap.toTauPicture() = TauPicture.fromBitmap(this)
fun Int.toTauPicture() = TauPicture.fromDrawableresource(this)
