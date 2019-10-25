// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.target.java

import com.intellij.execution.target.getRuntimeType
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.*

class JavaLanguageRuntimeUI(val config: JavaLanguageRuntimeConfiguration) :
  BoundConfigurable(config.displayName, config.getRuntimeType().helpTopic) {

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow()
      row("JDK home path:") {
        textField(config::homePath)
      }
      row("JDK version:") {
        textField(config::javaVersionString)
      }
    }
  }
}