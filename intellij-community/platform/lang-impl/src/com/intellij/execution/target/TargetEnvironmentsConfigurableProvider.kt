// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.target

import com.intellij.openapi.application.Experiments
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class TargetEnvironmentsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable? {
    return TargetEnvironmentsConfigurable(project)
  }

  override fun canCreateConfigurable(): Boolean = Experiments.getInstance().isFeatureEnabled("runtime.environments")
}