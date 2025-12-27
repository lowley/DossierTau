package lorry.dossiertau.data.dbModel

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@TypeConverters(FileIdConverter::class)
@Database(entities = [DiffEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {
    abstract fun fileDiffDao(): FileDiffDao
}