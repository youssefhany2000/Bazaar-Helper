package com.bazaarhelper.app.data.repository

import com.bazaarhelper.app.data.local.DailyRecordDao
import com.bazaarhelper.app.data.local.toEntity
import com.bazaarhelper.app.domain.model.DailyRecord
import com.bazaarhelper.app.domain.model.MonthlyReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

interface RecordRepository {
    fun getAllRecords(): Flow<List<DailyRecord>>
    fun getRecordByDateFlow(date: LocalDate): Flow<DailyRecord?>
    fun getMonthlyRecords(year: Int, month: Int): Flow<List<DailyRecord>>
    fun getMonthlyReport(year: Int, month: Int): Flow<MonthlyReport>
    suspend fun saveRecord(record: DailyRecord): Result<Unit>
    suspend fun updateRecord(record: DailyRecord): Result<Unit>
    suspend fun deleteRecord(id: Long): Result<Unit>
}

class RecordRepositoryImpl @Inject constructor(
    private val dao: DailyRecordDao
) : RecordRepository {

    override fun getAllRecords(): Flow<List<DailyRecord>> =
        dao.getAllRecords().map { list -> list.map { it.toDomain() } }

    override fun getRecordByDateFlow(date: LocalDate): Flow<DailyRecord?> =
        dao.getRecordByDateFlow(date.toString()).map { it?.toDomain() }

    override fun getMonthlyRecords(year: Int, month: Int): Flow<List<DailyRecord>> {
        val yearMonth = java.util.Locale.ROOT.let { locale ->
            "%04d-%02d".format(locale, year, month)
        }
        return dao.getRecordsByMonth(yearMonth).map { list -> list.map { it.toDomain() } }
    }

    override fun getMonthlyReport(year: Int, month: Int): Flow<MonthlyReport> {
        val currentYearMonth = java.util.Locale.ROOT.let { locale ->
            "%04d-%02d".format(locale, year, month)
        }
        
        val prevYear = if (month == 1) year - 1 else year
        val prevMonth = if (month == 1) 12 else month - 1
        val prevYearMonth = java.util.Locale.ROOT.let { locale ->
            "%04d-%02d".format(locale, prevYear, prevMonth)
        }

        return combine(
            dao.getRecordsByMonth(currentYearMonth),
            dao.getRecordsByMonth(prevYearMonth)
        ) { currentList, prevList ->
            val records = currentList.map { it.toDomain() }
            val totalSales = records.sumOf { it.sales }
            val totalPurchases = records.sumOf { it.purchases }
            val totalProfit = records.sumOf { it.profit }
            val daysCount = records.size

            val prevProfit = prevList.sumOf { it.sales - it.purchases }
            val profitChange = if (prevProfit != 0.0) {
                ((totalProfit - prevProfit) / kotlin.math.abs(prevProfit)) * 100
            } else null

            MonthlyReport(
                year = year,
                month = month,
                totalSales = totalSales,
                totalPurchases = totalPurchases,
                totalProfit = totalProfit,
                daysCount = daysCount,
                avgDailySales = if (daysCount > 0) totalSales / daysCount else 0.0,
                avgDailyProfit = if (daysCount > 0) totalProfit / daysCount else 0.0,
                records = records,
                profitChangePercentage = profitChange
            )
        }
    }

    override suspend fun saveRecord(record: DailyRecord): Result<Unit> = runCatching {
        dao.insertRecord(record.toEntity())
    }

    override suspend fun updateRecord(record: DailyRecord): Result<Unit> = runCatching {
        dao.updateRecord(record.toEntity())
    }

    override suspend fun deleteRecord(id: Long): Result<Unit> = runCatching {
        dao.deleteById(id)
    }
}
