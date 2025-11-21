package lorry.dossiertau.support.littleClasses

import androidx.compose.foundation.text.input.rememberTextFieldState
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@JvmInline
value class TauIdentifier(val value: UUID = UUID.randomUUID()) {

    companion object{
        fun random(): TauIdentifier = TauIdentifier()
    }

    fun equalsTo(other: TauIdentifier): Boolean = this.value.equals(other.value)

    override fun toString(): String {
        return super.toString()
    }

    fun toShortString(): String {
        return toString().take(5)
    }

}