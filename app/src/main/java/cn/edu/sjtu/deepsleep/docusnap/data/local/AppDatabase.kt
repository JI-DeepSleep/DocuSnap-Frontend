package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.edu.sjtu.deepsleep.docusnap.data.model.Converters

@Database(entities = [DocumentEntity::class, FormEntity::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun formDao(): FormDao
}