/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij

import com.intellij.application.options.CodeStyleConfigurableWrapper
import com.intellij.application.options.CodeStyleSchemesConfigurable
import com.intellij.application.options.codeStyle.CodeStyleSchemesModel
import com.intellij.application.options.codeStyle.CodeStyleSettingsPanelFactory
import com.intellij.application.options.codeStyle.NewCodeStyleSettingsPanel
import com.intellij.ide.todo.configurable.TodoConfigurable
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleScheme
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider

open class ConfigurableFactory : Disposable {
  companion object {
    @JvmStatic
    fun getInstance(): ConfigurableFactory {
      return ServiceManager.getService(ConfigurableFactory::class.java)
    }
  }

  override fun dispose() {
  }

  open fun createCodeStyleConfigurable(provider: CodeStyleSettingsProvider,
                                       codeStyleSchemesModel: CodeStyleSchemesModel,
                                       owner: CodeStyleSchemesConfigurable): CodeStyleConfigurableWrapper {
    val codeStyleConfigurableWrapper = CodeStyleConfigurableWrapper(provider, object : CodeStyleSettingsPanelFactory() {
      override fun createPanel(scheme: CodeStyleScheme): NewCodeStyleSettingsPanel {
        return NewCodeStyleSettingsPanel(
          provider.createConfigurable(scheme.codeStyleSettings, codeStyleSchemesModel.getCloneSettings(scheme)), codeStyleSchemesModel)
      }
    }, owner)
    return codeStyleConfigurableWrapper
  }

  open fun getTodoConfigurable(project: Project): TodoConfigurable {
    return TodoConfigurable()
  }
}