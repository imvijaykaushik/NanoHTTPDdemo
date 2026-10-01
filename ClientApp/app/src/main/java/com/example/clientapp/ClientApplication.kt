package com.example.clientapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt's entry point; generates the application-level dependency graph. */
@HiltAndroidApp
class ClientApplication : Application()
