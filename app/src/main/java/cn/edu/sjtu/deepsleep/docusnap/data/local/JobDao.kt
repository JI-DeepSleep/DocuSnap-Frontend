package cn.edu.sjtu.deepsleep.docusnap.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE status = 'pending' ORDER BY createdAt ASC")
    fun getPendingJobs(): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE status = 'processing' ORDER BY createdAt ASC")
    fun getProcessingJobs(): Flow<List<JobEntity>>
    
    @Insert
    suspend fun insertJob(job: JobEntity): Long
    
    @Update
    suspend fun updateJob(job: JobEntity)
    
    @Query("UPDATE jobs SET status = :status, result = :result, errorDetail = :errorDetail, updatedAt = :updatedAt WHERE id = :jobId")
    suspend fun updateJobStatus(jobId: Long, status: String, result: String? = null, errorDetail: String? = null, updatedAt: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteJob(job: JobEntity)
    
    @Query("DELETE FROM jobs WHERE status = 'completed' AND updatedAt < :timestamp")
    suspend fun deleteOldCompletedJobs(timestamp: Long): Int
} 