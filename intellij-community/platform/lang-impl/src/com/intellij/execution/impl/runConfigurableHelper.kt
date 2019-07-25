// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings

internal class RunConfigurationBean {
  val settings: RunnerAndConfigurationSettings
  val configurable: SingleConfigurationConfigurable<*>?

  constructor(settings: RunnerAndConfigurationSettings) {
    this.settings = settings
    configurable = null
  }

  constructor(configurable: SingleConfigurationConfigurable<*>) {
    this.configurable = configurable
    settings = this.configurable.settings
  }

  override fun toString() = settings.toString()
}

enum class RunConfigurableNodeKind {
  CONFIGURATION_TYPE, FOLDER, CONFIGURATION, TEMPORARY_CONFIGURATION, UNKNOWN;

  fun supportsDnD() = this == FOLDER || this == CONFIGURATION || this == TEMPORARY_CONFIGURATION

  val isConfiguration: Boolean
    get() = (this == CONFIGURATION) or (this == TEMPORARY_CONFIGURATION)
}

interface RunDialogBase {
  fun setOKActionEnabled(isEnabled: Boolean)

  val executor: Executor?

  fun setTitle(title: String)

  fun clickDefaultButton()
}