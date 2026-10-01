package com.example.serverapp.di

import com.example.serverapp.data.repository.HttpServerRepositoryImpl
import com.example.serverapp.data.service.ServiceServerController
import com.example.serverapp.data.repository.ServerStatusHolder
import com.example.serverapp.domain.repository.HttpServerRepository
import com.example.serverapp.domain.repository.MessageRepository
import com.example.serverapp.domain.repository.ServerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds domain interfaces to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHttpServerRepository(impl: HttpServerRepositoryImpl): HttpServerRepository

    /**
     * [ServerStatusHolder] is bound as [MessageRepository] so the route handler
     * can record requests, while the server repository injects the concrete type
     * to publish start/stop transitions.
     */
    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: ServerStatusHolder): MessageRepository

    /** Hosts the server in a foreground service so it survives app switching. */
    @Binds
    @Singleton
    abstract fun bindServerController(impl: ServiceServerController): ServerController
}
