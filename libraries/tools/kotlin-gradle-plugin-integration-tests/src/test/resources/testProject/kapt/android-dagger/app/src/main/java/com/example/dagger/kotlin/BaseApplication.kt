package com.example.dagger.kotlin

import android.app.Application

abstract class BaseApplication : Application() {

    protected fun initDaggerComponent(): ApplicationComponent {
        return DaggerApplicationComponent.builder().androidModule(AndroidModule(this)).build()
    }

}
