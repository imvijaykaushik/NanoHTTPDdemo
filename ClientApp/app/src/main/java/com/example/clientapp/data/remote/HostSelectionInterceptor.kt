package com.example.clientapp.data.remote

import com.example.clientapp.domain.model.ServerAddress
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites each request's host and port to the address the user entered.
 *
 * Retrofit fixes its `baseUrl` at build time, but here the target is only known
 * at runtime. Rebuilding Retrofit per request would work yet would throw away
 * OkHttp's connection pool each time; swapping the host in an interceptor keeps
 * one shared client.
 *
 * The address is held in an [AtomicReference] because OkHttp calls interceptors
 * on background threads while the UI thread updates the value.
 */
@Singleton
class HostSelectionInterceptor @Inject constructor() : Interceptor {

    private val address = AtomicReference<ServerAddress?>(null)

    fun setAddress(value: ServerAddress) {
        address.set(value)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val target = address.get() ?: return chain.proceed(request)

        val newUrl = request.url.newBuilder()
            .scheme("http")
            .host(target.host)
            .port(target.port)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
