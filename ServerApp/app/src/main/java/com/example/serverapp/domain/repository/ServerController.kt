package com.example.serverapp.domain.repository

import com.example.serverapp.domain.model.ServerConfig

/**
 * Starts and stops the server *process host* (on Android, a foreground service).
 *
 * Distinct from [HttpServerRepository], which binds the socket itself. The use
 * cases talk to this so they never reference Android's Service or Intent types,
 * and the ViewModel stays free of a Context.
 */
interface ServerController {

    fun requestStart(config: ServerConfig)

    fun requestStop()
}
