// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.statistic.eventLog

import com.intellij.configurationStore.jdomSerializer
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.eventLog.fus.FeatureUsageLogger
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.utils.PluginInfo
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.openapi.components.ReportValue
import com.intellij.openapi.components.SkipReportingStatistics
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.NonUrgentExecutor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xmlb.Accessor
import com.intellij.util.xmlb.BeanBinding
import org.jdom.Element
import java.util.concurrent.atomic.AtomicInteger

private val LOG = Logger.getInstance("com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEventPrinter")
private val GROUP = EventLogGroup("settings", 7)
private const val CHANGES_GROUP = "settings.changes"

private val recordedComponents: MutableSet<String> = ContainerUtil.newConcurrentSet()
private val recordedOptionNames: MutableSet<String> = ContainerUtil.newConcurrentSet()

internal fun isComponentNameWhitelisted(name: String): Boolean {
  return recordedComponents.contains(name)
}

internal fun isComponentOptionNameWhitelisted(name: String): Boolean {
  return recordedOptionNames.contains(name)
}

internal object FeatureUsageSettingsEvents {
  private val printer = FeatureUsageSettingsEventPrinter(false)

  fun logDefaultConfigurationState(componentName: String, clazz: Class<*>, project: Project?) {
    NonUrgentExecutor.getInstance().execute {
      if (FeatureUsageLogger.isEnabled()) {
        printer.logDefaultConfigurationState(componentName, clazz, project)
      }
    }
  }

  fun logConfigurationState(componentName: String, state: Any, project: Project?) {
    NonUrgentExecutor.getInstance().execute {
      if (FeatureUsageLogger.isEnabled()) {
        printer.logConfigurationState(componentName, state, project)
      }
    }
  }

  fun logConfigurationChanged(componentName: String, state: Any, project: Project?) {
    NonUrgentExecutor.getInstance().execute {
      if (FeatureUsageLogger.isEnabled()) {
        printer.logConfigurationStateChanged(componentName, state, project)
      }
    }
  }
}

open class FeatureUsageSettingsEventPrinter(private val recordDefault: Boolean) {
  private val valuesExtractor = ConfigurationStateExtractor(recordDefault)

  fun logDefaultConfigurationState(componentName: String, clazz: Class<*>, project: Project?) {
    try {
      if (recordDefault) {
        val default = jdomSerializer.getDefaultSerializationFilter().getDefaultValue(clazz)
        logConfigurationState(componentName, default, project)
      }
      else if (clazz != Element::class.java) {
        val pluginInfo = getPluginInfo(clazz)
        if (pluginInfo.isDevelopedByJetBrains()) {
          recordedComponents.add(componentName)
          logConfig(GROUP, "invoked", createComponentData(project, componentName, pluginInfo))
        }
      }
    }
    catch (e: Exception) {
      LOG.warn("Cannot initialize default settings for '$componentName'")
    }
  }

  fun logConfigurationStateChanged(componentName: String, state: Any?, project: Project?) {
    val (optionsValues, pluginInfo) = valuesExtractor.extract(project, componentName, state) ?: return
    val id = counter.incrementAndGet()
    for (data in optionsValues) {
      logSettingsChanged("component_changed_option", data, id)
    }

    if (!recordDefault) {
      logSettingsChanged("component_changed", createComponentData(project, componentName, pluginInfo), id)
    }
  }

  fun logConfigurationState(componentName: String, state: Any?, project: Project?) {
    val (optionsValues, pluginInfo) = valuesExtractor.extract(project, componentName, state) ?: return
    val eventId = if (recordDefault) "option" else "not.default"
    for (data in optionsValues) {
      logConfig(GROUP, eventId, data)
    }

    if (!recordDefault) {
      logConfig(GROUP, "invoked", createComponentData(project, componentName, pluginInfo))
    }
  }

  protected open fun logConfig(group: EventLogGroup, eventId: String, data: FeatureUsageData) {
    FeatureUsageLogger.logState(group, eventId, data.build())
  }

  protected open fun logSettingsChanged(eventId: String, data: FeatureUsageData, id: Int) {
    FUCounterUsageLogger.getInstance().logEvent(CHANGES_GROUP, eventId, data.addData("id", id))
  }

  companion object {
    private val counter = AtomicInteger(0)

    fun createComponentData(project: Project?, componentName: String, pluginInfo: PluginInfo): FeatureUsageData {
      val data = FeatureUsageData()
        .addData("component", componentName)
        .addPluginInfo(pluginInfo)
      if (project?.isDefault == true) {
        data.addData("default_project", true)
      }
      else {
        data.addProject(project)
      }
      return data
    }
  }
}

internal data class ConfigurationState(val optionsValues: List<FeatureUsageData>, val pluginInfo: PluginInfo)

internal data class ConfigurationStateExtractor(val recordDefault: Boolean) {
  internal fun extract(project: Project?, componentName: String, state: Any?): ConfigurationState? {
    if (state == null || state is Element) {
      return null
    }

    val pluginInfo = getPluginInfo(state.javaClass)
    if (!pluginInfo.isDevelopedByJetBrains()) {
      return null
    }

    val accessors = BeanBinding.getAccessors(state.javaClass)
    if (accessors.isEmpty()) {
      return null
    }

    recordedComponents.add(componentName)
    val optionsValues = accessors.mapNotNull { extractOptionValue(project, it, state, componentName, pluginInfo) }
    return ConfigurationState(optionsValues, pluginInfo)
  }

  private fun extractOptionValue(project: Project?,
                                 accessor: Accessor,
                                 state: Any,
                                 componentName: String,
                                 pluginInfo: PluginInfo): FeatureUsageData? {
    if (accessor.getAnnotation(SkipReportingStatistics::class.java) != null) {
      return null
    }
    val isDefault = !jdomSerializer.getDefaultSerializationFilter().accepts(accessor, state)
    if (!isDefault || recordDefault) {
      val data = FeatureUsageSettingsEventPrinter.createComponentData(project, componentName, pluginInfo)
      recordedOptionNames.add(accessor.name)
      data.addData("name", accessor.name)
      if (tryAddTypedValue(data, accessor, state)) {
        if (recordDefault) {
          data.addData("default", isDefault)
        }
        return data
      }
    }
    return null
  }

  private fun tryAddTypedValue(data: FeatureUsageData, accessor: Accessor, state: Any): Boolean {
    val type = accessor.genericType
    when {
      type === Boolean::class.javaPrimitiveType -> {
        data.addData("type", "bool")
        val value = accessor.readUnsafe(state) as? Boolean
        value?.let { data.addData("value", it) }
      }
      type === Int::class.javaPrimitiveType -> addValue<Int>(data, accessor, state, "int") { data.addData("value", it) }
      type === Long::class.javaPrimitiveType -> addValue<Long>(data, accessor, state, "int") { data.addData("value", it) }
      type === Float::class.javaPrimitiveType -> addValue<Float>(data, accessor, state, "float") { data.addData("value", it) }
      type === Double::class.javaPrimitiveType -> addValue<Double>(data, accessor, state, "float") { data.addData("value", it) }
      type is Class<*> && type.isEnum -> {
        data.addData("type", "enum")
        readValue(accessor, state) { (it as? Enum<*>)?.name }?.let { data.addData("value", it) }
      }
      type == String::class.java -> {
        data.addData("type", "string")
        val value = readValue(accessor, state) { value ->
          if (value is String && value in accessor.getAnnotation(ReportValue::class.java).possibleValues) {
            value
          }
          else null
        }
        value?.let { data.addData("value", it) }
      }
      else -> return false
    }
    return true
  }

  private inline fun <reified T> readValue(accessor: Accessor, state: Any, noinline transformValue: ((Any?) -> T?)? = null): T? {
    if (accessor.getAnnotation(ReportValue::class.java) != null) {
      val value = accessor.readUnsafe(state)
      return if (transformValue != null) {
        transformValue(value)
      }
      else {
        value as? T
      }
    }
    return null
  }

  private inline fun <reified T> addValue(data: FeatureUsageData, accessor: Accessor, state: Any, type: String, add: (T) -> Unit) {
    data.addData("type", type)
    val value =  readValue<T>(accessor, state)
    if (value != null) {
      add(value)
    }
  }
}
