package lorry.dossiertau.data.dbModel

import androidx.room.Insert
import androidx.room.Transaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.planes.DbCommand

class DiffRepository(
    private val dao: FileDiffDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun insertDiff(cmd: DbCommand, correlationId: String? = null) =
        withContext(io) { cmd.toDiff(correlationId)?.let { dao.insertDiff(it) }  }

    suspend fun insertDiffs(cmds: List<DbCommand>, correlationId: String? = null) =
        withContext(io) { dao.insertAllDiffs(cmds.mapNotNull {it.toDiff(correlationId)}) }

    fun getDiffsIn(folder: String) = dao.diffsForFolder(folder)

    @Insert
    suspend fun insertParentOnly(content: TauEntity.Content,): Long =
        withContext(io) { dao.insertContent(content) }

    @Insert
    suspend fun insertChildren(children: List<TauEntity.ContentItem>) =
        withContext(io) { dao.insertAllContentItems(children) }

    @Transaction
    suspend fun insertContent(parent: TauEntity.Content, children: List<TauEntity.ContentItem>) {
        val id = dao.insertContent(parent)
        if (children.isNotEmpty()) {
            val childrenWithId = children.map { it.copy(parentContentId = id) }
            dao.insertAllContentItems(childrenWithId)
        }
    }

    @Transaction
    suspend fun insertContents(parents: List<TauEntity.Content>) {
        parents.onEach { parent ->
            val id = dao.insertContent(parent)
            val children = parent.items
            if (children.isNotEmpty()) {
                val childrenWithId = children.map { it.copy(parentContentId = id) }
                dao.insertAllContentItems(childrenWithId)
            }
        }
    }
}