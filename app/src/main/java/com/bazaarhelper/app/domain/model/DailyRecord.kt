package com.bazaarhelper.app.domain.model

import java.time.LocalDate

data class DailyRecord(
    val id: Long = 0,
    val date: LocalDate,
    val sales: Double,
    val purchases: Double,
    val profit: Double = sales - purchases,
    val notes: String = ""
)

data class MonthlyReport(
    val year: Int,
    val month: Int,
    val totalSales: Double,
    val totalPurchases: Double,
    val totalProfit: Double,
    val daysCount: Int,
    val avgDailySales: Double,
    val avgDailyProfit: Double,
    val records: List<DailyRecord>,
    val profitChangePercentage: Double? = null
)
