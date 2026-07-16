package com.example.bmi.data.repository


import com.example.bmi.data.database.BmiRecord
import kotlinx.coroutines.flow.Flow

interface BmiRepository {
    suspend fun saveRecord(record: BmiRecord)
    fun observeLatestRecord(): Flow<BmiRecord?>
    fun observeAllRecords(): Flow<List<BmiRecord>>
    fun observeRecordsInRange(start: Long, end: Long): Flow<List<BmiRecord>>
    suspend fun hasAnyRecord(): Boolean
}