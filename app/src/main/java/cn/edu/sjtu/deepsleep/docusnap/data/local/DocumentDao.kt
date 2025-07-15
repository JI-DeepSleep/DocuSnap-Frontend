import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents")
    fun getAll(): Flow<List<DocumentEntity>>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}