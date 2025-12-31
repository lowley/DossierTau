package lorry.dossiertau.data.dbModel

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import lorry.dossiertau.data.dbModel.converters.DateConverters
import lorry.dossiertau.data.dbModel.converters.FileIdConverter

@TypeConverters(FileIdConverter::class, DateConverters::class)
@Database(entities = [DiffEntity::class], version = 2)
abstract class AppDb : RoomDatabase() {
    abstract fun fileDiffDao(): FileDiffDao
}