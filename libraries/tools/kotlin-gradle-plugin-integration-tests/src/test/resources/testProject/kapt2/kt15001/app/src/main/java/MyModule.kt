package com.example.kt15001

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MyModule(private val context: Context) {
  @Provides @Singleton fun provideContext() = context

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
