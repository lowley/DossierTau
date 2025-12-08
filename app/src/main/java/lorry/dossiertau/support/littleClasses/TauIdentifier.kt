package lorry.dossiertau.support.littleClasses

import androidx.compose.foundation.text.input.rememberTextFieldState
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class TauIdentifier @OptIn(ExperimentalUuidApi::class)
constructor(val value: Uuid = Uuid.random()) {

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun random(): TauIdentifier = TauIdentifier()
    }

    @OptIn(ExperimentalUuidApi::class)
    fun equalsTo(other: TauIdentifier): Boolean = this.value.toByteArray()
        .contentEquals(other.value.toByteArray())

    override fun toString(): String {
        return super.toString()
    }

    fun toShortString(): String {
        return toString().take(5)
    }

}

@OptIn(ExperimentalUuidApi::class)
fun String.toTauIdentifier(): TauIdentifier {

    if (this == null)
        return TauIdentifier.random()

    val result = try {
        TauIdentifier(Uuid.parse(this))
    } catch (ex: Exception) {
        println("String.toTauIdentifier(): erreur de conversion")
        TauIdentifier.random()
    }

    return result
}

@OptIn(ExperimentalUuidApi::class)
fun Uuid.toTauIdentifier(): TauIdentifier {

    if (this == null)
        return TauIdentifier.random()

    val result = try {
        TauIdentifier(this)
    } catch (ex: Exception) {
        println("Uuid.toTauIdentifier(): erreur de conversion")
        TauIdentifier.random()
    }

    return result
}