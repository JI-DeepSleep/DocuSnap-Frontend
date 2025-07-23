package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
    
    @Update
    suspend fun update(form: FormEntity)
    
    @Query("SELECT * FROM forms WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<FormEntity>
    
    @Query("DELETE FROM forms WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM forms WHERE " +
            "INSTR(LOWER(name), LOWER(:query)) > 0 OR " +
            "INSTR(LOWER(tags), LOWER(:query)) > 0")
    suspend fun searchByQuery(query: String): List<FormEntity>
}