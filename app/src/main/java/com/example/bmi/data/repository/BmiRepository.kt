package com.example.bmi.data.repository


import com.example.bmi.data.database.BmiRecord
import kotlinx.coroutines.flow.Flow

interface BmiRepository {
    suspend fun saveRecord(record: BmiRecord)
    fun observeLatestRecord(): Flow<BmiRecord?>
    fun observeAllRecords(): Flow<List<BmiRecord>>
    fun observeRecordsInRange(start: Long, end: Long): Flow<List<BmiRecord>>
    suspend fun hasAnyRecord(): Boolean

    suspend fun getRecordById(id: Long): BmiRecord?
    suspend fun deleteRecord(id: Long)

    suspend fun getRecordCount(): Int

    /**
     * 获取指定年月的每天最新 BMI 记录
     * 按 timeOfDay 优先级（Morning > Afternoon > Evening > Night）和 timestamp 降序取最新一条
     */
    suspend fun getMonthLatestRecords(year: Int, month: Int): List<BmiRecord>


    suspend fun getRecordsBetween(startTime: Long, endTime: Long): List<BmiRecord>
}