package lorry.dossiertau.data.PreferencesAppliFavo.support

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.fromBase64
import lorry.dossiertau.support.littleClasses.toTauPicture

object TauPictureSerializer : KSerializer<TauPicture> {
    // On définit que la forme sérialisée est une String
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TauPicture", PrimitiveKind.STRING)

    // Vers la String (Sérialisation)
    override fun serialize(encoder: Encoder, value: TauPicture) {
        // On utilise votre propriété .path existante qui renvoie la String ou "EMPTY"
        encoder.encodeString(value.toBase64())
    }

    // Depuis la String (Désérialisation)
    override fun deserialize(decoder: Decoder): TauPicture {
        val stringPath = decoder.decodeString()
        val result = fromBase64(stringPath)
        return result?.toTauPicture() ?: TauPicture.NONE
    }
}