package lorry.dossiertau.data.intelligenceService.utils2.events

import lorry.dossiertau.data.intelligenceService.utils2.repo.FileId

data class SnapshotElement(
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val fileId: FileId
)