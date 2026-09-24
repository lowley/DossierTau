package lorry.dossiertau.data.dbModel

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDiffDao {
    @Insert suspend fun insertDiff(diff: TauEntity.Diff): Long
    @Insert suspend fun insertContent(entity: TauEntity.Content): Long
    @Update suspend fun updateContent(entity: TauEntity.Content)
    @Insert suspend fun insertContentItem(item: TauEntity.ContentItem): Long
    @Insert suspend fun insertAllContentItems(items: List<TauEntity.ContentItem>): List<Long>
    @Insert suspend fun insertAllDiffs(diffs: List<TauEntity.Diff>): List<Long>

    @Query("""
        UPDATE content_items SET picture = :picture
        WHERE parentContentId = (
            SELECT contentId FROM folder_content
            WHERE full_path = :folderPath AND modifiedAtIso <> '1970-01-01T00:00:00Z'
            ORDER BY contentId DESC LIMIT 1
        ) AND name = :itemName
    """)
    suspend fun updateLatestContentItemPicture(folderPath: String, itemName: String, picture: ByteArray): Int

    /** Lit un seul BLOB à la fois afin de ne jamais remplir un CursorWindow avec
     * toutes les miniatures d'un dossier. */
    @Query("""
        SELECT picture FROM content_items
        WHERE parentContentId = (
            SELECT contentId FROM folder_content
            WHERE full_path = :folderPath AND modifiedAtIso <> '1970-01-01T00:00:00Z'
            ORDER BY contentId DESC LIMIT 1
        ) AND name = :itemName
        LIMIT 1
    """)
    suspend fun getLatestContentItemPicture(folderPath: String, itemName: String): ByteArray?

    @Query("""SELECT * FROM file_diffs
        WHERE (op_type='CREATE_ITEM' or op_type='MODIFY_ITEM' or op_type='DELETE_ITEM')
          AND full_path LIKE :folder ORDER BY modifiedAtIso DESC limit 1""")
    fun diffsForFolder(folder: String): Flow<List<TauEntity.Diff>>

    @Query("""SELECT * FROM file_diffs
        WHERE (op_type='CREATE_ITEM' or op_type='MODIFY_ITEM' or op_type='DELETE_ITEM')
          AND parentPath like :folder ORDER BY modifiedAtIso DESC limit 1""")
    fun diffsInParent(folder: String): Flow<List<TauEntity.Diff>>

    @Query("SELECT * FROM file_diffs ORDER BY diffId DESC limit 1")
    fun diffFlow(): Flow<TauEntity.Diff?>

    @Transaction
    @Query("""SELECT * FROM folder_content
        WHERE full_path = :folderPath
          AND modifiedAtIso <> '1970-01-01T00:00:00Z'
        ORDER BY contentId DESC limit 1""")
    fun getContentFlow(folderPath: String): Flow<List<ContentWithItems>>
}
