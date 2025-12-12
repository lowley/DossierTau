package lorry.dossiertau.data.dbModel

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import lorry.dossiertau.data.intelligenceService.utils.TransferingDecision
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.support.littleClasses.TauIdentifier
import lorry.dossiertau.support.littleClasses.TauItemName
import lorry.dossiertau.support.littleClasses.TauPath
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import lorry.dossiertau.support.littleClasses.toTauIdentifier
import lorry.dossiertau.support.littleClasses.toTauPath
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(
    tableName = "file_diffs",
    indices = [Index(value = ["full_path", "op_type"], unique = false)]
)
data class DiffEntity(
    @PrimaryKey(autoGenerate = true) val diffId: Long = 0L,
    val correlationId: String?,               // optionnel: TauIdentifier.toString()
    val op_type: String,                      // "CREATE_FILE" (plus tard: DELETE/RENAME…)
    val full_path: String,                    // TauPath normalisé (sans slash final)
    val modified_at_epoch_ms: Long,           // TauDate
    val item_type: String                     // "FILE" / "DIR" (ItemType)
)

@OptIn(ExperimentalUuidApi::class)
fun DbCommand.CreateItem.toFileDiffEntity(correlationId: String? = null): DiffEntity =
    DiffEntity(
        correlationId = correlationId ?: item.id.value.toString(),
        op_type = OpType.CreateFile.text,
        full_path = item.fullPath.path,
        modified_at_epoch_ms = item.modificationDate.value,
        item_type = item.type.name
    )

@OptIn(ExperimentalUuidApi::class)
fun DiffEntity.toTauItem(): TauItem {

    val item = when (this.op_type) {
        OpType.CreateFile.text -> {
            when (this.item_type) {
                ItemType.FILE.name ->
                    TauFile(
                        id = if (this.correlationId != null) this.correlationId.toTauIdentifier()
                        else Uuid.random().toTauIdentifier(),
                        fullPath = this.full_path.toTauPath(),
                        picture = TauPicture.NONE,
                        modificationDate = this.modified_at_epoch_ms.toTauDate()
                    )

                ItemType.FOLDER.name ->
                    TauFolder(
                        id = if (this.correlationId != null) this.correlationId.toTauIdentifier()
                        else Uuid.random().toTauIdentifier(),
                        fullPath = this.full_path.toTauPath(),
                        picture = TauPicture.NONE,
                        modificationDate = this.modified_at_epoch_ms.toTauDate()
                    )

                else -> TauFile.EMPTY
            }
        }

        else -> TauFile.EMPTY
    }

    return item
}

enum class OpType(val text: String) {
    CreateFile(text = "CREATE_FILE")


}

