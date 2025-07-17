package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity)

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: String): JobEntity?

    @Query("SELECT * FROM jobs WHERE status IN ('pending', 'processing')")
    fun getPendingJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE sha256 = :sha256")
    suspend fun getBySha256(sha256: String): JobEntity?

    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Update
    suspend fun update(job: JobEntity)

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM jobs WHERE status = 'completed' AND updatedAt < :timestamp")
    suspend fun deleteOldCompletedJobs(timestamp: Long)
} 