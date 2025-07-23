package cn.edu.sjtu.deepsleep.docusnap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.edu.sjtu.deepsleep.docusnap.data.model.Converters

@Database(
    entities = [DocumentEntity::class, FormEntity::class, JobEntity::class],
    version = 6  // Incremented version for usage tracking
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun formDao(): FormDao
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we're okay with destructive updates, we'll recreate the tables
                database.execSQL("DROP TABLE IF EXISTS jobs")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS jobs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        clientId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        sha256 TEXT NOT NULL,
                        hasContent INTEGER NOT NULL,
                        content TEXT,
                        aesKey TEXT,
                        status TEXT NOT NULL,
                        result TEXT,
                        errorDetail TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        plainAesKey TEXT
                    )
                """)

                // Also update documents and forms tables
                database.execSQL("ALTER TABLE documents ADD COLUMN job_id INTEGER")
                database.execSQL("ALTER TABLE forms ADD COLUMN job_id INTEGER")
            }
        }

        // Migration from version 5 to 6 - Add usage tracking columns
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add usage tracking columns to documents table
                database.execSQL("ALTER TABLE documents ADD COLUMN usage_count INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE documents ADD COLUMN last_used TEXT NOT NULL DEFAULT '2024-01-15'")
                
                // Add usage tracking columns to forms table
                database.execSQL("ALTER TABLE forms ADD COLUMN usage_count INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE forms ADD COLUMN last_used TEXT NOT NULL DEFAULT '2024-01-15'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docusnap_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()  // This will handle cases where migration fails
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}