import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DocumentEntity::class, FormEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun formDao(): FormDao
}