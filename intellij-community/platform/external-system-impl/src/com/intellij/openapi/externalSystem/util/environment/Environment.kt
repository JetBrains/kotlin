// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.environment

import com.intellij.openapi.components.service

interface Environment {

  fun property(name: String): String?

  fun variable(name: String): String?

  companion object {

    @JvmStatic
    fun getInstance() = service<Environment>()

    @JvmStatic
    fun getProperty(name: String) = getInstance().property(name)

    @JvmStatic
    fun getVariable(name: String) = getInstance().variable(name)
  }
}