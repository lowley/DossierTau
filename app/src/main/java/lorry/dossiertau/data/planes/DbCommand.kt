package lorry.dossiertau.data.planes

import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.support.littleClasses.TauDate
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauPath

sealed class DbCommand {

    data class CreateItem(
        val item: DbItem
    ): DbCommand()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }


}

data class DbItem(
    val id: TauIdentifier = TauIdentifier.random(),
    val fullPath: TauPath,
    val modificationDate: TauDate = TauDate.now(),
    val type: ItemType,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DbItem

        if (fullPath != other.fullPath) return false
        if (modificationDate != other.modificationDate) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fullPath.hashCode()
        result = 31 * result + modificationDate.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}