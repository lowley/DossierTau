package lorry.dossiertau.data.PreferencesAppliFavo.support

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauPath

object TauPathSerializer : KSerializer<TauPath> {
    // On définit que la forme sérialisée est une String
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TauPath", PrimitiveKind.STRING)

    // Vers la String (Sérialisation)
    override fun serialize(encoder: Encoder, value: TauPath) {
        // On utilise votre propriété .path existante qui renvoie la String ou "EMPTY"
        encoder.encodeString(value.path)
    }

    // Depuis la String (Désérialisation)
    override fun deserialize(decoder: Decoder): TauPath {
        val stringPath = decoder.decodeString()
        return if (stringPath == "EMPTY") {
            TauPath.EMPTY
        } else {
            stringPath.toTauPath() // Utilise votre extension String.toTauPath()
        }
    }
}