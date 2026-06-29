package com.bazaarhelper.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {

    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): DailyRecordEntity?

    @Query("SELECT * FROM daily_records WHERE date LIKE :yearMonth || '%' ORDER BY date ASC")
    fun getRecordsByMonth(yearMonth: String): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE date = :date LIMIT 1")
    fun getRecordByDateFlow(date: String): Flow<DailyRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyRecordEntity): Long

    @Update
    suspend fun updateRecord(record: DailyRecordEntity)

    @Delete
    suspend fun deleteRecord(record: DailyRecordEntity)

    @Query("DELETE FROM daily_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
