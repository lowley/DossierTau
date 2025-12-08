package lorry.dossiertau.data.dbModel

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DiffEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {
    abstract fun fileDiffDao(): FileDiffDao
}