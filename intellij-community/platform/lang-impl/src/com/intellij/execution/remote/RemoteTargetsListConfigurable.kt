// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class RemoteTargetsListConfigurable(project: Project, initialSelectedName: String? = null): SearchableConfigurable {
  @Suppress("unused")
  constructor(project: Project) : this(project, null)

  private val editor = RemoteTargetsMasterDetails(project, initialSelectedName)

  override fun isModified(): Boolean = editor.isModified

  override fun getId(): String = "Remote.Runners.Configurable"

  override fun getDisplayName(): String = "Remote Targets"

  override fun createComponent(): JComponent = editor.createComponent()

  override fun apply() {
    editor.apply()
  }

  override fun reset() {
    editor.reset()
  }

  override fun disposeUIResources() {
    editor.disposeUIResources()
    super.disposeUIResources()
  }

}