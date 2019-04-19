// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.statistic.eventLog

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.fus.FeatureUsageLogger
import com.intellij.internal.statistic.utils.getProjectId
import com.intellij.openapi.components.State
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.BeanBinding
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import org.jdom.Element
import java.util.*

private val LOG = Logger.getInstance("com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEventPrinter")
private val GROUP = EventLogGroup("settings", 2)

object FeatureUsageSettingsEvents {
  val printer = FeatureUsageSettingsEventPrinter(false)

  fun logDefaultConfigurationState(componentName: String, stateSpec: State, clazz: Class<*>, project: Project?) {
    if (stateSpec.reportStatistic && FeatureUsageLogger.isEnabled()) {
      printer.logDefaultConfigurationState(componentName, clazz, project)
    }
  }

  fun logConfigurationState(componentName: String, stateSpec: State, state: Any, project: Project?) {
    if (stateSpec.reportStatistic && FeatureUsageLogger.isEnabled()) {
      printer.logConfigurationState(componentName, state, project)
    }
  }
}

open class FeatureUsageSettingsEventPrinter(private val recordDefault: Boolean) {
  private val defaultFilter = SkipDefaultsSerializationFilter()

  fun logDefaultConfigurationState(componentName: String, clazz: Class<*>, project: Project?) {
    try {
      if (recordDefault) {
        val default = defaultFilter.getDefaultValue(clazz)
        logConfigurationState(componentName, default, project)
      }
      else {
        val isDefaultProject = project?.isDefault == true
        val hash = if (!isDefaultProject) toHash(project) else null
        logSettingCollectorWasInvoked(componentName, isDefaultProject, hash)
      }
    }
    catch (e: Exception) {
      LOG.warn("Cannot initialize default settings for '$componentName'")
    }
  }

  fun logConfigurationState(componentName: String, state: Any?, project: Project?) {
    if (state == null || state is Element) {
      return
    }

    val accessors = BeanBinding.getAccessors(state.javaClass)
    if (accessors.isEmpty()) {
      return
    }

    val isDefaultProject = project?.isDefault == true
    val hash = if (!isDefaultProject) toHash(project) else null

    for (accessor in accessors) {
      val type = accessor.genericType
      if (type === Boolean::class.javaPrimitiveType) {
        val value = accessor.read(state)
        val isNotDefault = defaultFilter.accepts(accessor, state)
        if (recordDefault || isNotDefault) {
          val content = HashMap<String, Any>()
          content["name"] = accessor.name
          content["value"] = value
          if (isNotDefault) {
            content["default"] = false
          }
          addProjectOptions(content, isDefaultProject, hash)
          logConfig(GROUP, componentName, content)
        }
      }
    }

    if (!recordDefault) {
      logSettingCollectorWasInvoked(componentName, isDefaultProject, hash)
    }
  }

  private fun addProjectOptions(content: HashMap<String, Any>,
                                isDefaultProject: Boolean,
                                projectHash: String?) {
    if (isDefaultProject) {
      content["default_project"] = true
    }
    else {
      projectHash?.let {
        content["project"] = projectHash
      }
    }
  }

  @Suppress("SameParameterValue")
  protected open fun logConfig(group: EventLogGroup, eventId: String, data: Map<String, Any>) {
    FeatureUsageLogger.logState(group, eventId, data)
  }

  private fun logSettingCollectorWasInvoked(componentName: String, isDefaultProject: Boolean, projectHash: String?) {
    val content = HashMap<String, Any>()
    content["invoked"] = true
    addProjectOptions(content, isDefaultProject, projectHash)
    logConfig(GROUP, componentName, content)
  }

  internal fun toHash(project: Project?): String? {
    return project?.let {
      return getProjectId(project)
    }
  }
}
