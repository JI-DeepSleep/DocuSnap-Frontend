package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
    
    @Update
    suspend fun update(document: DocumentEntity)
    
    @Query("SELECT * FROM documents WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<DocumentEntity>
    
    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
    
//    @Query("SELECT * FROM documents WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
//    suspend fun searchByQuery(query: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE " +
            "INSTR(LOWER(name), LOWER(:query)) > 0 OR " +
            "INSTR(LOWER(tags), LOWER(:query)) > 0")
    suspend fun searchByQuery(query: String): List<DocumentEntity>
}