// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.util.SmartList
import com.intellij.util.containers.filterSmartMutable
import com.intellij.util.containers.mapSmartSet

internal fun getEffectiveBeforeRunTaskList(ownTasks: List<BeforeRunTask<*>>, templateTasks: List<BeforeRunTask<*>>, ownIsOnlyEnabled: Boolean, isDisableTemplateTasks: Boolean): List<BeforeRunTask<*>> {
  val idToSet = ownTasks.mapSmartSet { it.providerId }
  val result = ownTasks.filterSmartMutable { !ownIsOnlyEnabled || it.isEnabled }
  var i = 0
  for (templateTask in templateTasks) {
    if (templateTask.isEnabled && !idToSet.contains(templateTask.providerId)) {
      val effectiveTemplateTask = if (isDisableTemplateTasks) {
        val clone = templateTask.clone()
        clone.isEnabled = false
        clone
      }
      else {
        templateTask
      }
      result.add(i, effectiveTemplateTask)
      i++
    }
  }
  return result
}

internal fun getHardcodedBeforeRunTasks(configuration: RunConfiguration, factory: ConfigurationFactory): List<BeforeRunTask<*>> {
  var result: MutableList<BeforeRunTask<*>>? = null
  for (provider in BeforeRunTaskProvider.EXTENSION_POINT_NAME.getExtensionList(configuration.project)) {
    val task = provider.createTask(configuration) ?: continue
    if (task.isEnabled) {
      factory.configureBeforeRunTaskDefaults(provider.id, task)
      if (task.isEnabled) {
        if (result == null) {
          result = SmartList<BeforeRunTask<*>>()
        }
        result.add(task)
      }
    }
  }
  return result.orEmpty()
}

internal data class BeforeRunTaskAndProvider(val task: BeforeRunTask<*>, val provider: BeforeRunTaskProvider<BeforeRunTask<*>>, val index: Int)