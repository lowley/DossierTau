package lorry.dossiertau.data.dbModel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lorry.dossiertau.data.planes.DbCommand
import lorry.dossiertau.support.littleClasses.toTauIdentifier

class DiffRepository(
    private val dao: FileDiffDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun insertDiff(cmd: DbCommand, correlationId: String? = null) =
        withContext(io) { dao.insert(cmd.toFileDiffEntity(correlationId)) }

    suspend fun insertDiffs(cmds: List<DbCommand>, correlationId: String? = null) =
        withContext(io) { dao.insertAll(cmds.map{it.toFileDiffEntity(correlationId)}) }

    fun getDiffsIn(folder: String) =
        dao.diffsForFolder(folder)
}