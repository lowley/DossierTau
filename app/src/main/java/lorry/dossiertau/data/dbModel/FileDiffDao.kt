package lorry.dossiertau.data.dbModel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDiffDao {
    @Insert
    suspend fun insert(diff: DiffEntity): Long

    // Pour l’écran : liste des diffs CREATE_FILE d’un dossier
    @Query("""
    SELECT * FROM file_diffs
    WHERE op_type='CREATE_FILE'
      AND full_path LIKE :folder || '/%'  -- naïf au début: "appartient au dossier"
    ORDER BY modified_at_epoch_ms DESC
  """)
    fun diffsForFolder(folder: String): Flow<List<DiffEntity>>

    @Query("""
    SELECT * FROM file_diffs
    ORDER BY modified_at_epoch_ms DESC
  """)
    fun dbForFolder(): Flow<List<DiffEntity>>
}