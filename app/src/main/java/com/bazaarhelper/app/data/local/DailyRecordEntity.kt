package com.bazaarhelper.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bazaarhelper.app.domain.model.DailyRecord
import java.time.LocalDate

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO format: yyyy-MM-dd
    val sales: Double,
    val purchases: Double,
    val profit: Double,
    val notes: String = ""
) {
    fun toDomain(): DailyRecord = DailyRecord(
        id = id,
        date = LocalDate.parse(date),
        sales = sales,
        purchases = purchases,
        profit = profit,
        notes = notes
    )
}

fun DailyRecord.toEntity(): DailyRecordEntity = DailyRecordEntity(
    id = id,
    date = date.toString(),
    sales = sales,
    purchases = purchases,
    profit = profit,
    notes = notes
)
