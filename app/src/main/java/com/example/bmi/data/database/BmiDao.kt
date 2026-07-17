package com.example.bmi.data.database


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BmiDao {
    @Insert
    suspend fun insert(record: BmiRecord)

    // 获取最新一条记录（按时间戳降序）
    @Query("SELECT * FROM bmi_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecord(): Flow<BmiRecord?>

    @Query("SELECT * FROM bmi_records ORDER BY timestamp DESC, createTime DESC")
    fun getAllRecords(): Flow<List<BmiRecord>>

    // 获取时间段内的记录（按时间戳升序）
    @Query("SELECT * FROM bmi_records WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp ASC")
    fun getRecordsInRange(start: Long, end: Long): Flow<List<BmiRecord>>

    // 获取记录总数
    @Query("SELECT EXISTS(SELECT 1 FROM bmi_records)")
    suspend fun hasAnyRecord(): Boolean

    // 删除所有记录（用于测试或清空功能）
    @Query("DELETE FROM bmi_records")
    suspend fun deleteAll()
}