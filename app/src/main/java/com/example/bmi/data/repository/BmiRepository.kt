package com.example.bmi.data.repository


import com.example.bmi.data.database.BmiRecord
import kotlinx.coroutines.flow.Flow

interface BmiRepository {
    suspend fun saveRecord(record: BmiRecord)
    fun observeLatestRecord(): Flow<BmiRecord?>
    fun observeAllRecords(): Flow<List<BmiRecord>>
    suspend fun hasAnyRecord(): Boolean

    suspend fun getRecordById(id: Long): BmiRecord?
    suspend fun deleteRecord(id: Long)

    suspend fun getRecordCount(): Int



    suspend fun getRecordsBetween(startTime: Long, endTime: Long): List<BmiRecord>


    suspend fun insertAll(records: List<BmiRecord>)
}