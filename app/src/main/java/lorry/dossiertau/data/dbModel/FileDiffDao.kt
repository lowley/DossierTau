package lorry.dossiertau.data.dbModel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDiffDao {
    @Insert
    suspend fun insertDiff(diff: TauEntity.Diff): Long

    @Insert
    suspend fun insertContent(entity: TauEntity.Content): Long

    @Update
    suspend fun updateContent(entity: TauEntity.Content)

    @Insert
    suspend fun insertContentItem(item: TauEntity.ContentItem): Long

    @Insert
    suspend fun insertAllContentItems(items: List<TauEntity.ContentItem>): List<Long>

    @Insert
    suspend fun insertAllDiffs(diffs: List<TauEntity.Diff>): List<Long>

    /**
     * Met à jour uniquement l'image de l'item dans le dernier snapshot Room du dossier.
     *
     * Cette écriture ciblée est utilisée par le chargement progressif : elle ne crée ni nouveau
     * Content, ni Diff, et ne modifie donc pas la sémantique du FileObserver/Spy.
     */
    @Query(
        """
        UPDATE content_items
        SET picture = :picture
        WHERE parentContentId = (
            SELECT contentId FROM folder_content
            WHERE full_path = :folderPath
              AND modifiedAtIso <> '1970-01-01T00:00:00Z'
            ORDER BY contentId DESC
            LIMIT 1
        )
        AND name = :itemName
        """
    )
    suspend fun updateLatestContentItemPicture(
        folderPath: String,
        itemName: String,
        picture: ByteArray
    ): Int

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
    fun diffsForFolder(folder: String): Flow<List<TauEntity.Diff>>

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
    fun diffsInParent(folder: String): Flow<List<TauEntity.Diff>>


    @Query(
        """
    SELECT * FROM file_diffs
    ORDER BY diffId DESC
    limit 1
  """
    )
    fun diffFlow(): Flow<TauEntity.Diff?>

    @Transaction
    @Query("""
        SELECT * FROM folder_content
        WHERE modifiedAtIso <> '1970-01-01T00:00:00Z'
        ORDER BY contentId DESC
        limit 1
        """)
    fun getAllContentFlow(): Flow<List<ContentWithItems>>
}