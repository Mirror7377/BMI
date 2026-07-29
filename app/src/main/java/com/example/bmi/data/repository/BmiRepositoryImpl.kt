package com.example.bmi.data.repository


import com.example.bmi.data.database.BmiDao
import com.example.bmi.data.database.BmiRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
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


    override suspend fun hasAnyRecord(): Boolean {
        return dao.hasAnyRecord()
    }

    override suspend fun getRecordById(id: Long): BmiRecord? {
        return dao.getRecordById(id)
    }

    override suspend fun deleteRecord(id: Long) {
        dao.deleteRecord(id)
    }

    //获取数据总条数
    override suspend fun getRecordCount(): Int {
        return dao.getRecordCount()
    }

    override suspend fun getRecordsBetween(
        startTime: Long,
        endTime: Long
    ): List<BmiRecord> {
        return dao.getRecordsBetween(startTime, endTime)
    }


    override suspend fun insertAll(records: List<BmiRecord>) {
        return dao.insertAll(records)
    }
}