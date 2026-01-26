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



@OptIn(ExperimentalUuidApi::class)
//suspend fun DbCommand.toDbEntity(correlationId: String? = null): Content {
//
//            val capsule = if (item.type == ItemType.FILE) FileCapsuleManager(item.fullPath.path, useOld = false).getCapsule()
//            else FolderCapsuleManager(item.fullPath, useOld = false).getCapsule()
//
//            val pictureBytes = capsule?.croppedPicture?.let { base64ToByteArray(it) }
//
//            Diff(
//                correlationId = correlationId ?: item.id.value.toString(),
//                op_type = OpType.CreateItem.text,
//                full_path = item.fullPath.path,
//                modifiedAtIso = ofEpochMilli(item.modificationDate.value),
//                item_type = item.type.name,
//                parentPath = item.fullPath.parentPath.path,
//                fileId = item.fileId,
//                pictureData = pictureBytes
//            )
//        }.invoke()
//
//
//    }
//
//    println ("SQL: va être envoyé Diff type ${result.op_type} avec path ${result.full_path}")
//    return result
//}

fun base64ToByteArray(base64String: String): ByteArray {
    return Base64.getDecoder().decode(base64String)
}