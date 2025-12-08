package lorry.dossiertau.data.dbModel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.support.littleClasses.toTauIdentifier

class DiffRepository(
    private val dao: FileDiffDao,
    private val io: CoroutineDispatcher
) {
    suspend fun insertDiff(cmd: DbCommand.CreateItem, correlationId: String? = null) =
        withContext(io) { dao.insert(cmd.toFileDiffEntity(correlationId)) }

    fun getDiffsIn(folder: String) =
        dao.diffsForFolder(folder)
}