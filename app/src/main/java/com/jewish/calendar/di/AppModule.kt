package com.jewish.calendar.di

import android.content.Context
import androidx.room.Room
import com.jewish.calendar.data.*
import com.jewish.calendar.data.SefariaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "yehuda_calendar_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideCycleDao(db: AppDatabase): CycleDao = db.cycleDao()

    @Provides
    @Singleton
    fun provideCleanDayDao(db: AppDatabase): CleanDayDao = db.cleanDayDao()

    @Provides
    @Singleton
    fun provideTevilahDao(db: AppDatabase): TevilahDao = db.tevilahDao()

    @Provides
    @Singleton
    fun provideCalendarEventDao(db: AppDatabase): CalendarEventDao = db.calendarEventDao()

    @Provides
    @Singleton
    fun provideHebrewCalendarRepository(): HebrewCalendarRepository = HebrewCalendarRepository()

    @Provides
    @Singleton
    fun provideZmanimRepository(): ZmanimRepository = ZmanimRepository()

    @Provides
    @Singleton
    fun provideClaudeRepository(): ClaudeRepository = ClaudeRepository()

    @Provides
    @Singleton
    fun provideSefariaRepository(): SefariaRepository = SefariaRepository()

    @Provides
    @Singleton
    fun provideAuthRepository(): com.jewish.calendar.data.AuthRepository = com.jewish.calendar.data.AuthRepository()

    @Provides
    @Singleton
    fun provideMikvehRepository(
        cycleDao: CycleDao,
        cleanDayDao: CleanDayDao,
        tevilahDao: TevilahDao
    ): MikvehRepository = MikvehRepository(cycleDao, cleanDayDao, tevilahDao)
}
