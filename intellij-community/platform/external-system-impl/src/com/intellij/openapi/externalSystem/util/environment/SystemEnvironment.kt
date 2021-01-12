// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.environment

import com.intellij.util.EnvironmentUtil

class SystemEnvironment : Environment {

  override fun property(name: String): String? {
    return System.getProperty(name)
  }

  override fun variable(name: String): String? {
    return EnvironmentUtil.getValue(name)
  }
}