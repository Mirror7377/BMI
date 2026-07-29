package com.example.bmi.data.database


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BmiDao {
    @Insert
    suspend fun insert(record: BmiRecord)

    // 获取最新一条记录（按时间戳-时间段降序）
    @Query("""
    SELECT * FROM bmi_records
    ORDER BY 
        DATE(timestamp / 1000, 'unixepoch') DESC,
        CASE timeOfDay
            WHEN 'Morning' THEN 1
            WHEN 'Afternoon' THEN 2
            WHEN 'Evening' THEN 3
            WHEN 'Night' THEN 4
            ELSE 0
        END DESC
    LIMIT 1;
""")
    fun getLatestRecord(): Flow<BmiRecord?>

    //最近记录的卡片排序
    @Query("""
    SELECT * FROM bmi_records
ORDER BY 
    DATE(timestamp / 1000, 'unixepoch') DESC,
    CASE timeOfDay
        WHEN 'Morning' THEN 1
        WHEN 'Afternoon' THEN 2
        WHEN 'Evening' THEN 3
        WHEN 'Night' THEN 4
        ELSE 0
    END DESC;
""")
    fun getAllRecords(): Flow<List<BmiRecord>>//unixepoch 把时间戳转换成日期格式（比如 2026-07-28）

    // 获取是否有数据
    @Query("SELECT EXISTS(SELECT 1 FROM bmi_records)")
    suspend fun hasAnyRecord(): Boolean


    //根据id查询数据
    @Query("SELECT * FROM bmi_records WHERE id = :id")
    suspend fun getRecordById(id: Long): BmiRecord?

    //根据id删除数据
    @Query("DELETE FROM bmi_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("SELECT COUNT(*) FROM bmi_records")
    suspend fun getRecordCount(): Int


    @Query("SELECT * FROM bmi_records WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getRecordsBetween(start: Long, end: Long): List<BmiRecord>

    //插入json解析后的数据
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<BmiRecord>)
}