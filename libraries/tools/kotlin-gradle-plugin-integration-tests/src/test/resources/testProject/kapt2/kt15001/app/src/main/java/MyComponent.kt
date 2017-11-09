package com.example.kt15001

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(MyModule::class))
interface MyComponent {
  fun inject(activity: MainActivity)
}
