// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.environment

import com.intellij.openapi.components.service

interface Environment {

  fun getProperty(name: String): String?

  fun getVariable(name: String): String?

  companion object {
    const val USER_HOME = "user.home"

    @JvmStatic
    fun getInstance() = service<Environment>()

    @JvmStatic
    fun getEnvProperty(name: String) = getInstance().getProperty(name)

    @JvmStatic
    fun getEnvVariable(name: String) = getInstance().getVariable(name)

    fun getUserHome() = getEnvProperty(USER_HOME)
  }
}