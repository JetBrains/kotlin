package com.example.dagger.kotlin

import com.example.dagger.kotlin.ui.HomeActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AndroidModule::class))
interface ApplicationComponent {
    fun inject(application: BaseApplication)
    fun inject(homeActivity: HomeActivity)
    fun inject(demoActivity: DemoActivity)
}