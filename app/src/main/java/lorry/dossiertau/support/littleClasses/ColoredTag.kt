package lorry.dossiertau.support.littleClasses

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ColoredTag(
    val id: UUID? = UUID.randomUUID(),
    val color: Color,
    val title: String,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColoredTag

        if (id != other.id) return false
        if (color != other.color) return false
        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + color.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }
}