package com.example.serverapp.core

import android.util.Log

/**
 * Tiny logging seam, so domain/data classes stay JVM-unit-testable
 * (android.util.Log is stubbed outside an emulator).
 */
interface Logger {
    fun d(message: String)
    fun w(message: String)
    fun e(message: String, throwable: Throwable? = null)
}

/** Writes to logcat under one tag: `adb logcat -s SERVER`. */
class AndroidLogger : Logger {
    override fun d(message: String) { Log.d(TAG, message) }
    override fun w(message: String) { Log.w(TAG, message) }
    override fun e(message: String, throwable: Throwable?) { Log.e(TAG, message, throwable) }

    private companion object { const val TAG = "SERVER" }
}

/** Used by unit tests; discards everything. */
class NoOpLogger : Logger {
    override fun d(message: String) = Unit
    override fun w(message: String) = Unit
    override fun e(message: String, throwable: Throwable?) = Unit
}
