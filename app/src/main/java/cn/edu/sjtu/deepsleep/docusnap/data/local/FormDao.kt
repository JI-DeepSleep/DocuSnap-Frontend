import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(form: FormEntity)

    @Query("SELECT * FROM forms WHERE id = :id")
    suspend fun getById(id: String): FormEntity?

    @Query("SELECT * FROM forms")
    fun getAll(): Flow<List<FormEntity>>

    @Query("DELETE FROM forms WHERE id = :id")
    suspend fun delete(id: String)
}