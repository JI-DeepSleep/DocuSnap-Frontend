import androidx.room.Database
import androidx.room.RoomDatabase
import cn.edu.sjtu.deepsleep.docusnap.data.local.DocumentDao
import cn.edu.sjtu.deepsleep.docusnap.data.local.FormDao

@Database(entities = [DocumentEntity::class, FormEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun formDao(): FormDao
}