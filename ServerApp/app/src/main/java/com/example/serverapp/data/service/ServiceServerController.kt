package com.example.serverapp.data.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.serverapp.domain.model.ServerConfig
import com.example.serverapp.domain.repository.ServerController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hosts the server in [HttpServerService] so it survives the user switching to
 * the Client app.
 */
@Singleton
class ServiceServerController @Inject constructor(
    @ApplicationContext private val context: Context,
) : ServerController {

    override fun requestStart(config: ServerConfig) {
        HttpServerService.start(context, config.port)
    }

    override fun requestStop() {
        HttpServerService.stop(context)
    }
}
