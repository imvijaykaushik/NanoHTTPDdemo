package com.example.clientapp.data.remote

import com.example.clientapp.data.dto.HealthResponseDto
import com.example.clientapp.data.dto.MessageRequestDto
import com.example.clientapp.data.dto.MessageResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit description of the server app's endpoints.
 *
 * The paths are relative. The real host and port are swapped in per-call by
 * [HostSelectionInterceptor], because the user types them at runtime.
 */
interface ServerApi {

    @GET("health")
    suspend fun health(): HealthResponseDto

    @POST("message")
    suspend fun sendMessage(@Body body: MessageRequestDto): MessageResponseDto
}
