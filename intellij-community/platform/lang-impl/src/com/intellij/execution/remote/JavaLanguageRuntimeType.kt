// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.*

class JavaLanguageRuntimeType : LanguageRuntimeType<JavaLanguageRuntimeConfiguration>(TYPE_ID) {
  override val icon = AllIcons.FileTypes.Java

  override val displayName = "Java"

  override val configurableDescription = "Configure Java"

  override fun isApplicableTo(runConfig: RunnerAndConfigurationSettings) = true

  override fun createDefaultConfig() = JavaLanguageRuntimeConfiguration()

  override fun createSerializer(config: JavaLanguageRuntimeConfiguration): PersistentStateComponent<*> = config

  override fun createConfigurable(project: Project, config: JavaLanguageRuntimeConfiguration): Configurable =
    JavaLanguageRuntimeUI(config)

  companion object {
    @JvmStatic
    val TYPE_ID = "JavaLanguageRuntime"
  }

  private class JavaLanguageRuntimeUI(val config: JavaLanguageRuntimeConfiguration)
    : BoundConfigurable(config.displayName, config.getRuntimeType().helpTopic) {

    override fun createPanel(): DialogPanel {
      return panel {
        titledRow()
        row("JDK home path:") {
          textField(config::homePath)
        }
        row("Application folder:") {
          textField(config::applicationFolder)
        }
      }
    }
  }
}