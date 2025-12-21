package lorry.dossiertau.data.dbModel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDiffDao {
    @Insert
    suspend fun insert(diff: DiffEntity): Long

    @Insert
    suspend fun insertAll(diffs: List<DiffEntity>): List<Long>

    // Pour l’écran : liste des diffs CREATE_FILE d’un dossier
    @Query(
        """
    SELECT * FROM file_diffs
    WHERE (op_type='CREATE_ITEM' or op_type='MODIFY_ITEM' or op_type='DELETE_ITEM')
      AND full_path LIKE :folder
          ORDER BY modified_at_epoch_ms DESC
          limit 1
  """
    )
    fun diffsForFolder(folder: String): Flow<List<DiffEntity>>

    // Pour l’écran : liste des diffs CREATE_FILE d’un dossier
    @Query(
        """
    SELECT * FROM file_diffs
    WHERE (op_type='CREATE_ITEM' or op_type='MODIFY_ITEM' or op_type='DELETE_ITEM')
      AND parentPath like :folder
          ORDER BY modified_at_epoch_ms DESC
          limit 1

  """
    )
    fun diffsInParent(folder: String): Flow<List<DiffEntity>>


    @Query(
        """
    SELECT * FROM file_diffs
    ORDER BY modified_at_epoch_ms DESC
    limit 1
  """
    )
    fun diffFlow(): Flow<DiffEntity?>
}