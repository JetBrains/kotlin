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
import com.intellij.openapi.util.JDOMExternalizable
import com.intellij.util.concurrency.NonUrgentExecutor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xmlb.Accessor
import com.intellij.util.xmlb.BeanBinding
import org.jdom.Element
import java.util.concurrent.atomic.AtomicInteger

private val LOG = Logger.getInstance("com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEventPrinter")
private val GROUP = EventLogGroup("settings", 9)
private const val CHANGES_GROUP = "settings.changes"
private const val ID_FIELD = "id"

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
          logConfig(GROUP, "invoked", createComponentData(project, componentName, pluginInfo), counter.incrementAndGet())
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
    val id = counter.incrementAndGet()
    for (data in optionsValues) {
      logConfig(GROUP, eventId, data, id)
    }

    if (!recordDefault) {
      logConfig(GROUP, "invoked", createComponentData(project, componentName, pluginInfo), id)
    }
  }

  protected open fun logConfig(group: EventLogGroup, eventId: String, data: FeatureUsageData, id: Int) {
    FeatureUsageLogger.logState(group, eventId, data.addData(ID_FIELD, id).build())
  }

  protected open fun logSettingsChanged(eventId: String, data: FeatureUsageData, id: Int) {
    FUCounterUsageLogger.getInstance().logEvent(CHANGES_GROUP, eventId, data.addData(ID_FIELD, id))
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
    if (state == null || state is Element || state is JDOMExternalizable) {
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
    if (isDefault && !recordDefault) {
      return null
    }
    val type = accessor.genericType
    return when {
      type === Boolean::class.javaPrimitiveType -> {
        val data = createOptionData(project, componentName, pluginInfo, accessor, isDefault, "bool")
        (accessor.readUnsafe(state) as? Boolean)?.let { data.addData("value", it) }
        data
      }
      type === Int::class.javaPrimitiveType -> {
        val data = createOptionData(project, componentName, pluginInfo, accessor, isDefault, "int")
        readValue<Int>(accessor, state)?.let { data.addData("value", it) }
        data
      }
      type === Long::class.javaPrimitiveType -> {
        val data = createOptionData(project, componentName, pluginInfo, accessor, isDefault, "int")
        readValue<Long>(accessor, state)?.let { data.addData("value", it) }
        data
      }
      type === Float::class.javaPrimitiveType -> {
        val data = createOptionData(project, componentName, pluginInfo, accessor, isDefault, "float")
        readValue<Float>(accessor, state)?.let { data.addData("value", it) }
        data
      }
      type === Double::class.javaPrimitiveType -> {
        val data = createOptionData(project, componentName, pluginInfo, accessor, isDefault, "float")
        readValue<Double>(accessor, state)?.let { data.addData("value", it) }
        data
      }
      type is Class<*> && type.isEnum -> {
        val data = createOptionData(project, componentName, pluginInfo, accessor, isDefault, "enum")
        readValue(accessor, state) { (it as? Enum<*>)?.name }?.let { data.addData("value", it) }
        data
      }
      type == String::class.java -> {
        val data = createOptionData(project, componentName, pluginInfo, accessor, isDefault, "string")
        val value = readValue(accessor, state) { value ->
          if (value is String && value in accessor.getAnnotation(ReportValue::class.java).possibleValues) {
            value
          }
          else null
        }
        value?.let { data.addData("value", it) }
        data
      }
      else -> null
    }
  }

  private fun createOptionData(project: Project?,
                               componentName: String,
                               pluginInfo: PluginInfo,
                               accessor: Accessor,
                               isDefault: Boolean,
                               type: String): FeatureUsageData {
    val data = FeatureUsageSettingsEventPrinter.createComponentData(project, componentName, pluginInfo)
    data.addData("type", type)
    data.addData("name", accessor.name)
    recordedOptionNames.add(accessor.name)
    if (recordDefault) {
      data.addData("default", isDefault)
    }
    return data
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

}
