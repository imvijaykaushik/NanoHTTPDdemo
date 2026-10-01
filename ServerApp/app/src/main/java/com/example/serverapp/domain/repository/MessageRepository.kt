package com.example.serverapp.domain.repository

import com.example.serverapp.domain.model.RequestLog

/**
 * Records what the server has handled, so the UI can display counts and the
 * last received message.
 */
interface MessageRepository {

    fun recordMessage(message: String)

    fun recordRequest(log: RequestLog)
}
