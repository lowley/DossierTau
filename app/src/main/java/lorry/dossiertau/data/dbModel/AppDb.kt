package lorry.dossiertau.data.dbModel

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import lorry.dossiertau.data.dbModel.converters.ContentItemConverter
import lorry.dossiertau.data.dbModel.converters.DateConverters
import lorry.dossiertau.data.dbModel.converters.FileIdConverter

@TypeConverters(
    FileIdConverter::class,
    DateConverters::class,
    ContentItemConverter::class
)
@Database(
    entities = [
        TauEntity.Diff::class,
        TauEntity.Content::class,
        TauEntity.ContentItem::class
    ], version = 4
)
abstract class AppDb : RoomDatabase() {
    abstract fun fileDiffDao(): FileDiffDao
}