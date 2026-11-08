package com.netcar.app.di

import com.netcar.app.data.repository.ExpenseRepository
import com.netcar.app.data.repository.ExpenseRepositoryImpl
import com.netcar.app.data.repository.FillUpRepository
import com.netcar.app.data.repository.FillUpRepositoryImpl
import com.netcar.app.data.repository.ReminderRepository
import com.netcar.app.data.repository.ReminderRepositoryImpl
import com.netcar.app.data.repository.ServiceRepository
import com.netcar.app.data.repository.ServiceRepositoryImpl
import com.netcar.app.data.repository.VehicleRepository
import com.netcar.app.data.repository.VehicleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVehicleRepository(
        impl: VehicleRepositoryImpl
    ): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindFillUpRepository(
        impl: FillUpRepositoryImpl
    ): FillUpRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        impl: ServiceRepositoryImpl
    ): ServiceRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        impl: ReminderRepositoryImpl
    ): ReminderRepository
}
