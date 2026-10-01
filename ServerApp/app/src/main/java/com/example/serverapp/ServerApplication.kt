package com.example.serverapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt's entry point; generates the application-level dependency graph. */
@HiltAndroidApp
class ServerApplication : Application()
