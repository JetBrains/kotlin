// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType.*
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle.message
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.*
import org.jetbrains.annotations.Nls
import kotlin.reflect.KMutableProperty0

class ExternalSystemGroupConfigurable(project: Project) : BoundConfigurable(message("settings.build.tools.display.name")) {

  private val settings = ExternalSystemProjectTrackerSettings.getInstance(
    project)

  override fun createPanel() = panel {
    row {
      radioButtonGroup(
        property = settings::autoReloadType,
        title = message("settings.build.tools.auto.reload.radio.button.group.title")
      ) {
        row {
          radioButton(
            value = ALL,
            text = message("settings.build.tools.auto.reload.radio.button.all.label"),
            comment = message("settings.build.tools.auto.reload.radio.button.all.comment")
          )
        }
        row {
          radioButton(
            value = SELECTIVE,
            text = message("settings.build.tools.auto.reload.radio.button.selective.label"),
            comment = message("settings.build.tools.auto.reload.radio.button.selective.comment")
          )
        }
        row {
          radioButton(
            value = NONE,
            text = message("settings.build.tools.auto.reload.radio.button.none.label"),
            comment = message("settings.build.tools.auto.reload.radio.button.none.comment")
          )
        }
      }
    }
  }

  companion object {
    fun <T> BaseBuilder.radioButtonGroup(
      property: KMutableProperty0<T>,
      title: String? = null,
      init: RadioButtonGroupBuilder<T>.() -> Unit
    ) {
      buttonGroup(title) {
        RadioButtonGroupBuilder(
          property).init()
      }
    }

    class RadioButtonGroupBuilder<T>(private val property: KMutableProperty0<T>) {
      fun Cell.radioButton(value: T, @Nls text: String, @Nls comment: String? = null) {
        radioButton(text, { property.get() == value }, { if (it) property.set(value) }, comment)
      }
    }
  }
}