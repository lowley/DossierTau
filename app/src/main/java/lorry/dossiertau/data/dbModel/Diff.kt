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

    @Entity(
        tableName = "file_diffs",
        indices = [Index(value = ["full_path", "op_type"], unique = false)]
    )
    data class Diff(
        @PrimaryKey(autoGenerate = true) val diffId: Long = 0L,
        val correlationId: String?,               // optionnel: TauIdentifier.toString()
        val op_type: String,                      // "CREATE_FILE" (plus tard: DELETE/RENAME…)
        val full_path: String,                    // TauPath normalisé (sans slash final)
        val modifiedAtIso: Instant?,           // TauDate
        val item_type: String,
        val parentPath: String, // "FILE" / "DIR" (ItemType)
        val fileId: FileId = FileId.EMPTY,
        val pictureData: ByteArray? = null
    ) : TauEntity() {

        fun display(): String {
            return "⏵ $op_type ↈ $item_type ↈ $full_path ↈ $modifiedAtIso ↈ \uD83D\uDDBD ${pictureData != null} ⏴"
        }
    }

    @Entity(tableName = "folder_content")
    data class Content(
        @PrimaryKey(autoGenerate = true) val contentId: Long = 0L,
        val correlationId: String?,
        val full_path: String,
        val modifiedAtIso: Instant?
    ) : TauEntity() {

        // On place 'items' en dehors du constructeur principal
        // Room l'ignorera totalement lors de la création de la table
        @Ignore
        var items: List<ContentItem> = emptyList()

        @Ignore
        constructor(
            contentId: Long = 0L,
            correlationId: String?,
            full_path: String,
            modifiedAtIso: Instant?,
            items: List<ContentItem>
        ) : this(contentId, correlationId, full_path, modifiedAtIso) {
            this.items = items
        }

        fun display(): String {
            return "⏵ $full_path ↈ $modifiedAtIso ↈ ⌸ {${items.size} ⏴"
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
        val fileId: FileId = FileId.EMPTY,
        val type: ItemType
    ) : TauEntity()
}

@OptIn(ExperimentalUuidApi::class)
suspend fun DbCommand.toDiff(correlationId: String? = null): TauEntity.Diff? {

    assert(this !is DbCommand.GlobalRefresh)

    val result = when (this) {
        is DbCommand.CreateItem -> suspend {
            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(
                item.fullPath.path,
                useOld = false
            ).getCapsule()
            else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()

            val pictureBytes = capsule?.croppedPicture?.let { base64ToByteArray(it) }

            TauEntity.Diff(
                correlationId = correlationId ?: item.id.value.toString(),
                op_type = OpType.CreateItem.text,
                full_path = item.fullPath.path,
                modifiedAtIso = Instant.ofEpochMilli(item.modificationDate.value),
                item_type = item.type.name,
                parentPath = item.fullPath.parentPath.path,
                fileId = item.fileId,
                pictureData = pictureBytes
            )
        }.invoke()

        is DbCommand.DeleteItem -> TauEntity.Diff(
            correlationId = correlationId ?: item.id.value.toString(),
            op_type = OpType.DeleteItem.text,
            full_path = item.fullPath.path,
            modifiedAtIso = Instant.ofEpochMilli(item.modificationDate.value),
            item_type = item.type.name,
            parentPath = item.fullPath.parentPath.path,
            fileId = item.fileId
        )

        is DbCommand.ModifyItem -> suspend {

            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(
                item.fullPath.path,
                useOld = false
            ).getCapsule()
            else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()

            val pictureBytes = capsule?.croppedPicture?.let { base64ToByteArray(it) }

            TauEntity.Diff(
                correlationId = correlationId ?: item.id.value.toString(),
                op_type = OpType.ModifyItem.text,
                full_path = item.fullPath.path,
                modifiedAtIso = Instant.ofEpochMilli(item.modificationDate.value),
                item_type = item.type.name,
                parentPath = item.fullPath.parentPath.path,
                fileId = item.fileId,
                pictureData = pictureBytes
            )
        }.invoke()

//        is DbCommand.GlobalRefresh -> TauEntity.Content(
//            correlationId = correlationId,
//            full_path = path.path,
//            modifiedAtIso = Instant.ofEpochMilli(refreshDate.value),
//            items = this.items.map { it.toContentItem(0L) },
//        )

        else -> null
    }

    return result
}

@OptIn(ExperimentalUuidApi::class)
suspend fun DbCommand.GlobalRefresh.toEntity(correlationId: String? = null): TauEntity.Content {

    val result = TauEntity.Content(
        correlationId = correlationId,
        full_path = this.path.path,
        modifiedAtIso = Instant.ofEpochMilli(this.refreshDate.value),
        items = this.items.map { it.toContentItem(0L) },
    )

    return result
}

@OptIn(ExperimentalUuidApi::class)
fun TauEntity.Diff.toTauItem(): TauItem {

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
fun Long.epochMillisToDateTime(zone: ZoneId = ZoneId.systemDefault()): String {
    val fmt = DateTimeFormatter.ofPattern(dateTimePattern)
    return Instant.ofEpochMilli(this).atZone(zone).format(fmt)
}


fun String.dateTimetoEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long {
    val fmt = DateTimeFormatter.ofPattern(dateTimePattern)
    val ldt = LocalDateTime.parse(this, fmt)
    return ldt.atZone(zone).toInstant().toEpochMilli()
}

data class ContentWithItems(
    @Embedded val content: TauEntity.Content,
    @Relation(
        parentColumn = "contentId",
        entityColumn = "parentContentId"
    )
    val items: List<TauEntity.ContentItem>
)