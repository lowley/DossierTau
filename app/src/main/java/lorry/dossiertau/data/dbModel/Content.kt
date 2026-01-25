package lorry.dossiertau.data.dbModel

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import lorry.dossiertau.data.intelligenceService.utils.events.ItemType
import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.data.model.TauFile
import lorry.dossiertau.data.model.TauFolder
import lorry.dossiertau.data.model.TauItem
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.parentPath
import lorry.dossiertau.support.littleClasses.path
import lorry.dossiertau.support.littleClasses.toTauDate
import lorry.dossiertau.support.littleClasses.toTauIdentifier
import lorry.dossiertau.support.littleClasses.toTauPath
import lorry.dossiertau.ui.support.capsule.utilities.FileCapsuleManager
import lorry.dossiertau.ui.support.capsule.utilities.FolderCapsuleManager
import java.time.Instant
import java.time.Instant.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(
    tableName = "folder_content",
//    indices = [Index(value = ["full_path", "op_type"], unique = false)]
)
data class Content(
    @PrimaryKey(autoGenerate = true) val contentId: Long = 0L,
    val correlationId: String?,               // optionnel: TauIdentifier.toString()
    val full_path: String,                    // TauPath normalisé (sans slash final)
    val modifiedAtIso: Instant?,           // TauDate
    val fileId: FileId = FileId.EMPTY,
    val items: List<ContentItem> = emptyList()
){

    fun display(): String{
        return "⏵ $op_type ↈ $item_type ↈ $full_path ↈ $modifiedAtIso ↈ \uD83D\uDDBD ${pictureData!= null} ⏴"
    }
}

@Entity(
    tableName = "content_items",
    foreignKeys = [
        ForeignKey(
            entity = Content::class,
            parentColumns = ["contentId"],
            childColumns = ["parentContentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentContentId")]
)
data class ContentItem(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0L,
    //lien inter-tables
    val parentContentId: Long,
    val id: String?,
    val name: String,
    val picture: ByteArray? = null,
    val memo: String?,
    val modificationDate: Instant?,
    val fileId: FileId = FileId.EMPTY
)

@OptIn(ExperimentalUuidApi::class)
suspend fun DbCommand.toContent(correlationId: String? = null): Content {

            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(item.fullPath.path, useOld = false).getCapsule()
            else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()

            val pictureBytes = capsule?.croppedPicture?.let { base64ToByteArray(it) }

            Diff(
                correlationId = correlationId ?: item.id.value.toString(),
                op_type = OpType.CreateItem.text,
                full_path = item.fullPath.path,
                modifiedAtIso = ofEpochMilli(item.modificationDate.value),
                item_type = item.type.name,
                parentPath = item.fullPath.parentPath.path,
                fileId = item.fileId,
                pictureData = pictureBytes
            )
        }.invoke()


    }

    println ("SQL: va être envoyé Diff type ${result.op_type} avec path ${result.full_path}")
    return result
}

@OptIn(ExperimentalUuidApi::class)
fun Diff.toTauItem(): TauItem {

    val item = when (this.op_type) {
        OpType.CreateItem.text,
        OpType.DeleteItem.text,
        OpType.ModifyItem.text -> {
            when (this.item_type) {
                ItemType.FILE.name ->
                    TauFile.of(
                        id = if (this.correlationId != null) this.correlationId.toTauIdentifier()
                        else Uuid.random().toTauIdentifier(),
                        fullPath = this.full_path.toTauPath(),
                        picture = TauPicture.NONE,
                        modificationDate = (this.modifiedAtIso?.toEpochMilli() ?: 0L)
                            .toTauDate(),
                        fileId = this.fileId
                    )

                ItemType.FOLDER.name ->
                    TauFolder(
                        id = if (this.correlationId != null) this.correlationId.toTauIdentifier()
                        else Uuid.random().toTauIdentifier(),
                        fullPath = this.full_path.toTauPath(),
                        picture = TauPicture.NONE,
                        modificationDate = (this.modifiedAtIso?.toEpochMilli() ?: 0L)
                            .toTauDate(),
                        fileId = this.fileId
                    )

                else -> TauFile.EMPTY
            }
        }

        else -> TauFile.EMPTY
    }

    return item
}

enum class OpType(val text: String) {
    CreateItem(text = "CREATE_ITEM"),
    DeleteItem(text = "DELETE_ITEM"),
    ModifyItem(text = "MODIFY_ITEM"),
    FolderRefresh(text = "GLOBAL_REFRESH"),

}

val dateTimePattern = "dd/MM/yyyy HH:mm:ss + AAAA"
fun Long.epochMillisToDateTime(zone: ZoneId = ZoneId.systemDefault()): String{
    val fmt = DateTimeFormatter.ofPattern(dateTimePattern)
    return ofEpochMilli(this).atZone(zone).format(fmt)
}


fun String.dateTimetoEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long {
    val fmt = DateTimeFormatter.ofPattern(dateTimePattern)
    val ldt = LocalDateTime.parse(this, fmt)
    return ldt.atZone(zone).toInstant().toEpochMilli()
}

fun base64ToByteArray(base64String: String): ByteArray {
    return Base64.getDecoder().decode(base64String)
}