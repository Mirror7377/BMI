package com.example.bmi.data.repository


import com.example.bmi.data.database.BmiDao
import com.example.bmi.data.database.BmiRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BmiRepositoryImpl @Inject constructor(
    private val dao: BmiDao
) : BmiRepository {

    override suspend fun saveRecord(record: BmiRecord) {
        dao.insert(record)
    }

    override fun observeLatestRecord(): Flow<BmiRecord?> {
        return dao.getLatestRecord()
    }

    override fun observeAllRecords(): Flow<List<BmiRecord>> {
        return dao.getAllRecords()
    }

    override fun observeRecordsInRange(start: Long, end: Long): Flow<List<BmiRecord>> {
        return dao.getRecordsInRange(start, end)
    }

    // BmiRepository.kt
    override suspend fun hasAnyRecord(): Boolean {
        return dao.hasAnyRecord()
    }
}