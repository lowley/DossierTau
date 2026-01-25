package lorry.dossiertau.data.dbModel

import androidx.room.Entity
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
){

    fun display(): String{
        return "⏵ $op_type ↈ $item_type ↈ $full_path ↈ $modifiedAtIso ↈ \uD83D\uDDBD ${pictureData!= null} ⏴"
    }
}

@OptIn(ExperimentalUuidApi::class)
suspend fun DbCommand.toFileDiffEntity(correlationId: String? = null): Diff {

    val result = when (this) {
        is DbCommand.CreateItem -> suspend {
            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(item.fullPath.path, useOld = false).getCapsule()
            else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()

            val pictureBytes = capsule?.croppedPicture?.let { base64ToByteArray(it) }

            Diff(
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

        is DbCommand.DeleteItem -> Diff(
            correlationId = correlationId ?: item.id.value.toString(),
            op_type = OpType.DeleteItem.text,
            full_path = item.fullPath.path,
            modifiedAtIso = Instant.ofEpochMilli(item.modificationDate.value),
            item_type = item.type.name,
            parentPath = item.fullPath.parentPath.path,
            fileId = item.fileId
        )

        is DbCommand.ModifyItem -> suspend {

            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(item.fullPath.path, useOld = false).getCapsule()
                else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()

            val pictureBytes = capsule?.croppedPicture?.let { base64ToByteArray(it) }

            Diff(
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

        is DbCommand.GlobalRefresh -> Diff(
            correlationId = correlationId,
            op_type = OpType.FolderRefresh.text,
            full_path = path.path,
            modifiedAtIso = Instant.ofEpochMilli(refreshDate.value),
            item_type = ItemType.FOLDER.name,
            parentPath = path.parentPath.path,
        )
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
    return Instant.ofEpochMilli(this).atZone(zone).format(fmt)
}


fun String.dateTimetoEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long {
    val fmt = DateTimeFormatter.ofPattern(dateTimePattern)
    val ldt = LocalDateTime.parse(this, fmt)
    return ldt.atZone(zone).toInstant().toEpochMilli()
}

fun base64ToByteArray(base64String: String): ByteArray {
    return Base64.getDecoder().decode(base64String)
}