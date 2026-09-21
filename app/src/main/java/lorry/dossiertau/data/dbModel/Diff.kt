package lorry.dossiertau.data.dbModel

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class TauEntity() {
    @Entity(tableName = "file_diffs", indices = [Index(value = ["full_path", "op_type"], unique = false)])
    data class Diff(
        @PrimaryKey(autoGenerate = true) val diffId: Long = 0L,
        val correlationId: String?, val op_type: String, val full_path: String,
        val modifiedAtIso: Instant?, val item_type: String, val parentPath: String,
        val fileId: FileId = FileId.EMPTY, val pictureData: ByteArray? = null
    ) : TauEntity() { fun display() = "⏵ $op_type ↈ $item_type ↈ $full_path ↈ $modifiedAtIso ↈ 🖽 ${pictureData != null} ⏴" }

    @Entity(tableName = "folder_content")
    data class Content(
        @PrimaryKey(autoGenerate = true) val contentId: Long = 0L,
        val correlationId: String?, val full_path: String, val modifiedAtIso: Instant?
    ) : TauEntity() {
        @Ignore var items: List<ContentItem> = emptyList()
        @Ignore constructor(contentId: Long = 0L, correlationId: String?, full_path: String, modifiedAtIso: Instant?, items: List<ContentItem>) : this(contentId, correlationId, full_path, modifiedAtIso) { this.items = items }
        fun display() = "⏵ $full_path ↈ $modifiedAtIso ↈ ⌸ {${items.size} ⏴"
    }

    @Entity(tableName = "content_items", foreignKeys = [ForeignKey(entity = Content::class, parentColumns = ["contentId"], childColumns = ["parentContentId"], onDelete = ForeignKey.CASCADE)], indices = [Index("parentContentId")])
    data class ContentItem(
        @PrimaryKey(autoGenerate = true) val itemId: Long = 0L,
        val parentContentId: Long, val id: String?, val name: String,
        val picture: ByteArray? = null, val memo: String?, val modificationDate: Instant?,
        val fileId: FileId = FileId.EMPTY, val type: ItemType
    ) : TauEntity()
}

/** Metadata légère utilisée pour afficher un dossier.
 * La colonne picture est volontairement exclue : les BLOB sont lus individuellement
 * à la demande par le chargeur progressif de miniatures.
 */
data class ContentItemMetadata(
    val itemId: Long,
    val parentContentId: Long,
    val id: String?,
    val name: String,
    val memo: String?,
    val modificationDate: Instant?,
    val fileId: FileId,
    val type: ItemType,
)

@OptIn(ExperimentalUuidApi::class)
suspend fun DbCommand.toDiff(correlationId: String? = null): TauEntity.Diff? {
    assert(this !is DbCommand.GlobalRefresh)
    return when (this) {
        is DbCommand.CreateItem -> {
            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(item.fullPath.path, useOld = false).getCapsule() else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()
            TauEntity.Diff(correlationId ?: item.id.value.toString(), OpType.CreateItem.text, item.fullPath.path, Instant.ofEpochMilli(item.modificationDate.value), item.type.name, item.fullPath.parentPath.path, item.fileId, capsule?.croppedPicture?.let { base64ToByteArray(it) })
        }
        is DbCommand.DeleteItem -> TauEntity.Diff(correlationId ?: item.id.value.toString(), OpType.DeleteItem.text, item.fullPath.path, Instant.ofEpochMilli(item.modificationDate.value), item.type.name, item.fullPath.parentPath.path, item.fileId)
        is DbCommand.ModifyItem -> {
            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(item.fullPath.path, useOld = false).getCapsule() else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()
            TauEntity.Diff(correlationId ?: item.id.value.toString(), OpType.ModifyItem.text, item.fullPath.path, Instant.ofEpochMilli(item.modificationDate.value), item.type.name, item.fullPath.parentPath.path, item.fileId, capsule?.croppedPicture?.let { base64ToByteArray(it) })
        }
        else -> null
    }
}

@OptIn(ExperimentalUuidApi::class)
suspend fun DbCommand.GlobalRefresh.toEntity(correlationId: String? = null): TauEntity.Content = TauEntity.Content(correlationId = correlationId, full_path = path.path, modifiedAtIso = Instant.ofEpochMilli(refreshDate.value), items = items.map { it.toContentItem(0L) })

@OptIn(ExperimentalUuidApi::class)
fun TauEntity.Diff.toTauItem(): TauItem = when (op_type) {
    OpType.CreateItem.text, OpType.DeleteItem.text, OpType.ModifyItem.text -> when (item_type) {
        ItemType.FILE.name -> TauFile.of(id = correlationId?.toTauIdentifier() ?: Uuid.random().toTauIdentifier(), fullPath = full_path.toTauPath(), picture = TauPicture.NONE, modificationDate = (modifiedAtIso?.toEpochMilli() ?: 0L).toTauDate(), fileId = fileId)
        ItemType.FOLDER.name -> TauFolder(id = correlationId?.toTauIdentifier() ?: Uuid.random().toTauIdentifier(), fullPath = full_path.toTauPath(), picture = TauPicture.NONE, modificationDate = (modifiedAtIso?.toEpochMilli() ?: 0L).toTauDate(), fileId = fileId)
        else -> TauFile.EMPTY
    }
    else -> TauFile.EMPTY
}

enum class OpType(val text: String) { CreateItem("CREATE_ITEM"), DeleteItem("DELETE_ITEM"), ModifyItem("MODIFY_ITEM"), FolderRefresh("GLOBAL_REFRESH") }
val dateTimePattern = "dd/MM/yyyy HH:mm:ss + AAAA"
fun Long.epochMillisToDateTime(zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(this).atZone(zone).format(DateTimeFormatter.ofPattern(dateTimePattern))
fun String.dateTimetoEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long { val ldt = LocalDateTime.parse(this, DateTimeFormatter.ofPattern(dateTimePattern)); return ldt.atZone(zone).toInstant().toEpochMilli() }

data class ContentWithItems(
    @Embedded val content: TauEntity.Content,
    @Relation(
        parentColumn = "contentId",
        entityColumn = "parentContentId",
        entity = TauEntity.ContentItem::class,
        projection = ["itemId", "parentContentId", "id", "name", "memo", "modificationDate", "fileId", "type"]
    )
    val items: List<ContentItemMetadata>
)
