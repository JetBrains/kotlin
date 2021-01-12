// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions

import com.intellij.openapi.application.Experiments
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class ReaderModeConfigurableProvider(val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable {
    return ReaderModeConfigurable(project)
  }

  override fun canCreateConfigurable() = Experiments.getInstance().isFeatureEnabled("editor.reader.mode")
}