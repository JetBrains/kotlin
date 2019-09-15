// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import javax.swing.Icon

abstract class BaseExtendableType<C : BaseExtendableConfiguration>(val id: String) {

  abstract val displayName: String

  abstract val icon: Icon

  abstract fun createSerializer(config: C): PersistentStateComponent<*>

  abstract fun createDefaultConfig(): C

  fun duplicateConfig(config: C): C = createDefaultConfig().also {
    XmlSerializerUtil.copyBean(config, it)
  }

  abstract fun createConfigurable(project: Project, config: C): Configurable

  open val helpTopic: String? = null

  @Suppress("UNCHECKED_CAST")
  internal fun castConfiguration(config: BaseExtendableConfiguration) = config as C
}