// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType.*
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle.message
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.*
import org.jetbrains.annotations.Nls
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.jvm.jvmName

class ExternalSystemGroupConfigurable(private val project: Project) : BoundConfigurable(message("settings.build.tools.display.name")) {

  override fun createPanel() = panel {
    val settings = ExternalSystemProjectTrackerSettings.getInstance(project)
    row {
      buttonGroup(settings::autoReloadType, message("settings.build.tools.auto.reload.radio.button.group.title"), NONE) {
        row {
          radioButton(message("settings.build.tools.auto.reload.radio.button.all.label"), ALL)
        }
        row {
          radioButton(message("settings.build.tools.auto.reload.radio.button.selective.label"), SELECTIVE)
            .comment(message("settings.build.tools.auto.reload.radio.button.selective.comment"))
        }
      }
    }
  }

  private inline fun <reified T : Enum<T>> Row.buttonGroup(
    prop: KMutableProperty0<T>,
    @Nls title: String,
    defaultValue: T,
    crossinline init: RowBuilderWithButtonGroupProperty<T>.() -> Unit
  ) {
    val key = ExternalSystemGroupConfigurable::class.jvmName
    val propertiesComponent = PropertiesComponent.getInstance(project)
    buttonGroup(prop, title, defaultValue, key, propertiesComponent, init)
  }

  private inline fun <reified T : Enum<T>> Row.buttonGroup(
    prop: KMutableProperty0<T>,
    @Nls title: String,
    defaultValue: T,
    name: String,
    propertiesComponent: PropertiesComponent,
    crossinline init: RowBuilderWithButtonGroupProperty<T>.() -> Unit
  ) {
    val previousValue by lazy { propertiesComponent.getValue(name)?.let { enumValueOf<T>(it) } }
    var isEnabled = prop.get() != defaultValue
    var value = if (isEnabled) prop.get() else previousValue ?: defaultValue
    checkBox(title)
      .apply { attachSubRowsEnabled(component) }
      .withSelectedBinding(PropertyBinding({ isEnabled }, { isEnabled = it }))
    buttonGroup(PropertyBinding({ value }, { value = it }), init)
    onGlobalApply {
      prop.set(if (isEnabled) value else defaultValue)
      propertiesComponent.setValue(name, value.toString())
    }
  }
}