package lorry.dossiertau.data.dbModel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDiffDao {
    @Insert
    suspend fun insert(diff: Diff): Long

    @Insert
    suspend fun insertAll(diffs: List<Diff>): List<Long>

    // Pour l’écran : liste des diffs CREATE_FILE d’un dossier
    @Query(
        """
    SELECT * FROM file_diffs
    WHERE (op_type='CREATE_ITEM' or op_type='MODIFY_ITEM' or op_type='DELETE_ITEM')
      AND full_path LIKE :folder
          ORDER BY modifiedAtIso DESC
          limit 1
  """
    )
    fun diffsForFolder(folder: String): Flow<List<Diff>>

    // Pour l’écran : liste des diffs CREATE_FILE d’un dossier
    @Query(
        """
    SELECT * FROM file_diffs
    WHERE (op_type='CREATE_ITEM' or op_type='MODIFY_ITEM' or op_type='DELETE_ITEM')
      AND parentPath like :folder
          ORDER BY modifiedAtIso DESC
          limit 1

  """
    )
    fun diffsInParent(folder: String): Flow<List<Diff>>


    @Query(
        """
    SELECT * FROM file_diffs
    ORDER BY diffId DESC
    limit 1
  """
    )
    fun diffFlow(): Flow<Diff?>
}