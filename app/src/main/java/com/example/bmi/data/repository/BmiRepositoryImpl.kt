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

    override fun observeRecordsInRange(start: Long, end: Long): Flow<List<BmiRecord>> {
        return dao.getRecordsInRange(start, end)
    }

    // BmiRepository.kt
    override suspend fun hasAnyRecord(): Boolean {
        return dao.hasAnyRecord()
    }

    override suspend fun getRecordById(id: Long): BmiRecord? {
        return dao.getRecordById(id)
    }

    override suspend fun deleteRecord(id: Long) {
        dao.deleteRecord(id)
    }

    override suspend fun getRecordCount(): Int {
        return dao.getRecordCount()
    }

    override suspend fun getMonthLatestRecords(year: Int, month: Int): List<BmiRecord> {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endTime = cal.timeInMillis
        return dao.getMonthLatestRecords(startTime, endTime)
    }
}