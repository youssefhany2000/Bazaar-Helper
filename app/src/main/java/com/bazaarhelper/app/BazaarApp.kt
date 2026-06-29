package com.bazaarhelper.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BazaarApp : Application() // Triggering re-compilation
