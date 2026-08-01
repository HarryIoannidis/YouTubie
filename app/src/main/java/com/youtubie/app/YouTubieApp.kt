package com.youtubie.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point that enables Hilt dependency injection for YouTubie.
 */
@HiltAndroidApp
class YouTubieApp : Application()
