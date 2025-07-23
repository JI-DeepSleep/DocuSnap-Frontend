package cn.edu.sjtu.deepsleep.docusnap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.edu.sjtu.deepsleep.docusnap.data.model.Converters

@Database(
    entities = [DocumentEntity::class, FormEntity::class, JobEntity::class],
    version = 1,  // Reset to version 1 since we're dropping all data on schema changes
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun formDao(): FormDao
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docusnap_database"
                )
                    .fallbackToDestructiveMigration(false)  // Drop all data when schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}