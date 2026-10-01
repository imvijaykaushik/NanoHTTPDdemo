package com.example.clientapp.di

import com.example.clientapp.data.repository.ServerRepositoryImpl
import com.example.clientapp.domain.repository.ServerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the domain interface to its data-layer implementation. This is the
 * seam that lets presentation depend only on the abstraction.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository
}
