package com.netcar.app.di

import android.content.Context
import androidx.room.Room
import com.netcar.app.data.local.AppDatabase
import com.netcar.app.data.local.dao.ExpenseDao
import com.netcar.app.data.local.dao.FillUpDao
import com.netcar.app.data.local.dao.ReminderDao
import com.netcar.app.data.local.dao.ServiceDao
import com.netcar.app.data.local.dao.VehicleDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideVehicleDao(database: AppDatabase): VehicleDao = database.vehicleDao()

    @Provides
    fun provideFillUpDao(database: AppDatabase): FillUpDao = database.fillUpDao()

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideServiceDao(database: AppDatabase): ServiceDao = database.serviceDao()

    @Provides
    fun provideReminderDao(database: AppDatabase): ReminderDao = database.reminderDao()
}
