// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.statistic.eventLog

import com.intellij.configurationStore.jdomSerializer
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.fus.FeatureUsageLogger
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.internal.statistic.utils.getProjectId
import com.intellij.openapi.components.ReportValue
import com.intellij.openapi.components.State
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.NonUrgentExecutor
import com.intellij.util.containers.ContainerUtil
import com.intellij.serialization.MutableAccessor
import com.intellij.util.xmlb.BeanBinding
import org.jdom.Element
import java.util.*

private val LOG = Logger.getInstance("com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEventPrinter")
private val GROUP = EventLogGroup("settings", 4)

private val recordedComponents: MutableSet<String> = ContainerUtil.newConcurrentSet()
private val recordedOptionNames: MutableSet<String> = ContainerUtil.newConcurrentSet()

fun isComponentNameWhitelisted(name: String): Boolean {
  return recordedComponents.contains(name)
}

fun isComponentOptionNameWhitelisted(name: String): Boolean {
  return recordedOptionNames.contains(name)
}

object FeatureUsageSettingsEvents {
  val printer = FeatureUsageSettingsEventPrinter(false)

  fun logDefaultConfigurationState(componentName: String, stateSpec: State, clazz: Class<*>, project: Project?) {
    if (stateSpec.reportStatistic && FeatureUsageLogger.isEnabled()) {
      NonUrgentExecutor.getInstance().execute {
        printer.logDefaultConfigurationState(componentName, clazz, project)
      }
    }
  }

  fun logConfigurationState(componentName: String, stateSpec: State, state: Any, project: Project?) {
    if (stateSpec.reportStatistic && FeatureUsageLogger.isEnabled()) {
      NonUrgentExecutor.getInstance().execute {
        printer.logConfigurationState(componentName, state, project)
      }
    }
  }
}

open class FeatureUsageSettingsEventPrinter(private val recordDefault: Boolean) {
  fun logDefaultConfigurationState(componentName: String, clazz: Class<*>, project: Project?) {
    try {
      if (recordDefault) {
        val default = jdomSerializer.getDefaultSerializationFilter().getDefaultValue(clazz)
        logConfigurationState(componentName, default, project)
      }
      else if (clazz != Element::class.java && getPluginInfo(clazz).isDevelopedByJetBrains()) {
        recordedComponents.add(componentName)
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

    val pluginInfo = getPluginInfo(state.javaClass)
    if (!pluginInfo.isDevelopedByJetBrains()) {
      return
    }

    val accessors = BeanBinding.getAccessors(state.javaClass)
    if (accessors.isEmpty()) {
      return
    }

    recordedComponents.add(componentName)
    val eventId: String = if (recordDefault) "option" else "not.default"
    val isDefaultProject = project?.isDefault == true
    val hash = if (!isDefaultProject) toHash(project) else null

    for (accessor in accessors) {
      val type = accessor.genericType
      if (type === Boolean::class.javaPrimitiveType) {
        logConfigValue(accessor, state, "bool", eventId, isDefaultProject, true, hash, componentName)
      }
      else if (type === Int::class.javaPrimitiveType || type === Long::class.javaPrimitiveType) {
        val reportValue = accessor.getAnnotation(ReportValue::class.java) != null
        logConfigValue(accessor, state, "int", eventId, isDefaultProject, reportValue, hash, componentName)
      }
      else if (type === Float::class.javaPrimitiveType || type === Double::class.javaPrimitiveType) {
        val reportValue = accessor.getAnnotation(ReportValue::class.java) != null
        logConfigValue(accessor, state, "float", eventId, isDefaultProject, reportValue, hash, componentName)
      }
    }

    if (!recordDefault) {
      logSettingCollectorWasInvoked(componentName, isDefaultProject, hash)
    }
  }

  private fun logConfigValue(accessor: MutableAccessor,
                             state: Any,
                             type: String,
                             eventId: String,
                             isDefaultProject: Boolean,
                             reportValue: Boolean,
                             hash: String?,
                             componentName: String) {
    val value = accessor.readUnsafe(state)
    val isDefault = !jdomSerializer.getDefaultSerializationFilter().accepts(accessor, state)
    if (!isDefault || recordDefault) {
      recordedOptionNames.add(accessor.name)
      val content = HashMap<String, Any>()
      content["type"] = type
      content["component"] = componentName
      content["name"] = accessor.name
      if (reportValue) {
        content["value"] = value
      }
      if (recordDefault) {
        content["default"] = isDefault
      }
      addProjectOptions(content, isDefaultProject, hash)
      logConfig(GROUP, eventId, content)
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
    content["component"] = componentName
    addProjectOptions(content, isDefaultProject, projectHash)
    logConfig(GROUP, "invoked", content)
  }

  internal fun toHash(project: Project?): String? {
    return project?.let {
      return getProjectId(project)
    }
  }
}
