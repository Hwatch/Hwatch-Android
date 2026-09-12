package com.hwatch.app

import android.app.Application
import coil3.SingletonImageLoader
import com.hwatch.app.di.AppContainer

class HWatchApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SingletonImageLoader.setSafe { container.imageLoader }
    }
}
