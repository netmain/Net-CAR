package com.netcar.app.di

import com.netcar.app.data.NetCarRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetCarRepository(): NetCarRepository {
        return NetCarRepository()
    }
}
