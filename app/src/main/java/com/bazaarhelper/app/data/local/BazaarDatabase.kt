package com.bazaarhelper.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BazaarDatabase : RoomDatabase() {
    abstract fun dailyRecordDao(): DailyRecordDao
}
