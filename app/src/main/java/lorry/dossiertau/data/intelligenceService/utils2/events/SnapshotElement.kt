package lorry.dossiertau.data.intelligenceService.utils2.events

import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId
import lorry.dossiertau.support.littleClasses.TauPicture
import lorry.dossiertau.support.littleClasses.TauPicture.Bitmap
data class SnapshotElement(
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val fileId: FileId,
    val picture: Bitmap? = null,
    val memo: String? = null
)