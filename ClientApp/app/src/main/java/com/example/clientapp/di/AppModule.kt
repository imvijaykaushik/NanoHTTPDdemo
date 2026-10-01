package com.example.clientapp.di

import com.example.clientapp.core.AndroidLogger
import com.example.clientapp.core.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Injected rather than referenced directly so unit tests can substitute a
     * test dispatcher and run coroutines deterministically.
     */
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideLogger(): Logger = AndroidLogger()
}
