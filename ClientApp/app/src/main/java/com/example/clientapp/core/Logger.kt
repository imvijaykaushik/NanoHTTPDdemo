package com.example.clientapp.core

import android.util.Log

/**
 * Tiny logging seam.
 *
 * Interface rather than calling [Log] directly so that use cases and
 * repositories stay unit-testable on the JVM, where android.util.Log is stubbed.
 */
interface Logger {
    fun d(message: String)
    fun w(message: String)
    fun e(message: String, throwable: Throwable? = null)
}

/** Writes to logcat under a single tag, so `adb logcat -s CLIENT` shows the flow. */
class AndroidLogger : Logger {
    override fun d(message: String) { Log.d(TAG, message) }
    override fun w(message: String) { Log.w(TAG, message) }
    override fun e(message: String, throwable: Throwable?) { Log.e(TAG, message, throwable) }

    private companion object { const val TAG = "CLIENT" }
}

/** Used by unit tests; discards everything. */
class NoOpLogger : Logger {
    override fun d(message: String) = Unit
    override fun w(message: String) = Unit
    override fun e(message: String, throwable: Throwable?) = Unit
}
