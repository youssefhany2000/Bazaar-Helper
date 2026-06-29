package com.bazaarhelper.app.di

import android.content.Context
import androidx.room.Room
import com.bazaarhelper.app.data.local.BazaarDatabase
import com.bazaarhelper.app.data.local.DailyRecordDao
import com.bazaarhelper.app.data.repository.RecordRepository
import com.bazaarhelper.app.data.repository.RecordRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BazaarDatabase =
        Room.databaseBuilder(context, BazaarDatabase::class.java, "BazaarHelper.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDailyRecordDao(db: BazaarDatabase): DailyRecordDao = db.dailyRecordDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordRepository(impl: RecordRepositoryImpl): RecordRepository
}
