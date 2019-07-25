// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.compound

import com.intellij.execution.*
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.compareTypesForUi
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Icon
import kotlin.collections.LinkedHashMap

data class SettingsAndEffectiveTarget(val configuration: RunConfiguration, val target: ExecutionTarget)

class CompoundRunConfiguration @JvmOverloads constructor(name: String? = null,
                                                         project: Project,
                                                         factory: ConfigurationFactory = runConfigurationType<CompoundRunConfigurationType>()) :
  RunConfigurationMinimalBase<CompoundRunConfigurationOptions>(name, factory, project), RunnerIconProvider, WithoutOwnBeforeRunSteps, Cloneable {
  companion object {
    @JvmField
    internal val COMPARATOR: Comparator<RunConfiguration> = Comparator { o1, o2 ->
      val compareTypeResult = compareTypesForUi(o1.type, o2.type)
      if (compareTypeResult == 0) o1.name.compareTo(o2.name) else compareTypeResult
    }
  }

  // have to use RunConfiguration instead of RunnerAndConfigurationSettings because setConfigurations (called from CompoundRunConfigurationSettingsEditor.applyEditorTo) cannot use RunnerAndConfigurationSettings
  private var sortedConfigurationsWithTargets = LinkedHashMap<RunConfiguration, ExecutionTarget?>()
  private var isInitialized = false
  private val isDirty = AtomicBoolean()

  fun getConfigurationsWithTargets(runManager: RunManagerImpl): Map<RunConfiguration, ExecutionTarget?> {
    initIfNeeded(runManager)
    return sortedConfigurationsWithTargets
  }

  fun setConfigurationsWithTargets(value: Map<RunConfiguration, ExecutionTarget?>) {
    markInitialized()

    sortedConfigurationsWithTargets.clear()
    sortedConfigurationsWithTargets.putAll(value)
    isDirty.set(true)
  }

  fun setConfigurationsWithoutTargets(value: Collection<RunConfiguration>) {
    setConfigurationsWithTargets(value.associate { it to null })
  }

  private fun initIfNeeded(runManager: RunManagerImpl) {
    if (isInitialized) {
      return
    }

    doInit(runManager)
  }

  @TestOnly
  fun doInit(runManager: RunManagerImpl) {
    sortedConfigurationsWithTargets.clear()

    val targetManager = ExecutionTargetManager.getInstance(project) as ExecutionTargetManagerImpl

    for (item in options.configurations) {
      val type = item.type
      val name = item.name
      if (type == null || name == null) {
        continue
      }

      val settings = runManager.findConfigurationByTypeAndName(type, name)?.configuration
      if (settings != null && settings !== this) {
        val target = item.targetId?.let { targetManager.findTargetByIdFor(settings, it) }
        sortedConfigurationsWithTargets.put(settings, target)
      }
    }

    markInitialized()
  }

  private fun markInitialized() {
    isInitialized = true
  }

  override fun getConfigurationEditor() = CompoundRunConfigurationSettingsEditor(project)

  override fun checkConfiguration() {
    if (sortedConfigurationsWithTargets.isEmpty()) {
      throw RuntimeConfigurationException("There is nothing to run")
    }

    if (ExecutionTargetManager.getInstance(project).getTargetsFor(this).isEmpty()) {
      throw RuntimeConfigurationException("No suitable targets to run on; please choose a target for each configuration")
    }
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    try {
      checkConfiguration()
    }
    catch (e: RuntimeConfigurationException) {
      throw ExecutionException(e.message)
    }

    promptUserToUseRunDashboard(project, getConfigurationsWithEffectiveRunTargets().map {
      it.configuration.type
    })

    return RunProfileState { _, _ ->
      ApplicationManager.getApplication().invokeLater {
        val groupId = ExecutionEnvironment.getNextUnusedExecutionId()
        for ((configuration, target) in getConfigurationsWithEffectiveRunTargets()) {
          val settings = RunManagerImpl.getInstanceImpl(project).findSettings(configuration) ?: continue
          ExecutionUtil.runConfiguration(settings, executor, target, groupId)
        }
      }
      null
    }
  }

  fun getConfigurationsWithEffectiveRunTargets(): List<SettingsAndEffectiveTarget> {
    val activeTarget = ExecutionTargetManager.getActiveTarget(project)
    val defaultTarget = DefaultExecutionTarget.INSTANCE

    return sortedConfigurationsWithTargets.mapNotNull { (configuration, specifiedTarget) ->
      val effectiveTarget = specifiedTarget ?: if (ExecutionTargetManager.canRun(configuration, activeTarget)) activeTarget else defaultTarget
      SettingsAndEffectiveTarget(configuration, effectiveTarget)
    }
  }

  override fun getState(): CompoundRunConfigurationOptions? {
    if (isDirty.compareAndSet(true, false)) {
      options.configurations = sortedConfigurationsWithTargets.mapTo(ArrayList()) { entry ->
        TypeNameTarget(entry.key.type.id, entry.key.name, entry.value?.id)
      }
    }
    return options
  }

  override fun loadState(state: CompoundRunConfigurationOptions) {
    super.loadState(state)
    sortedConfigurationsWithTargets.clear()
    isInitialized = false
  }

  override fun clone(): RunConfiguration {
    val clone = CompoundRunConfiguration(name, project, factory)
    clone.loadState(state!!)
    return clone
  }

  override fun getExecutorIcon(configuration: RunConfiguration, executor: Executor): Icon {
    return when {
      DefaultRunExecutor.EXECUTOR_ID == executor.id && hasRunningSingletons() -> AllIcons.Actions.Restart
      else -> executor.icon
    }
  }

  private fun hasRunningSingletons(): Boolean {
    val project = project
    if (project.isDisposed) {
      return false
    }

    return ExecutionManagerImpl.getInstance(project).getRunningDescriptors { s ->
      val manager = RunManagerImpl.getInstanceImpl(project)
      for ((configuration, _) in sortedConfigurationsWithTargets) {
        if (configuration is CompoundRunConfiguration && configuration.hasRunningSingletons()) {
          return@getRunningDescriptors true
        }

        val settings = manager.findSettings(configuration)
        if (settings != null && !settings.configuration.isAllowRunningInParallel && configuration == s.configuration) {
          return@getRunningDescriptors true
        }
      }
      false
    }.isNotEmpty()
  }
}

class CompoundRunConfigurationOptions : BaseState() {
  @get:XCollection()
  @get:Property(surroundWithTag = false)
  var configurations by list<TypeNameTarget>()
}

@Tag("toRun")
@Property(style = Property.Style.ATTRIBUTE)
class TypeNameTarget() : BaseState() {
  constructor(type: String, name: String, targetId: String?) : this() {
    this.type = type
    this.name = name
    this.targetId = targetId
  }

  var type by string()
  var name by string()
  var targetId by string()
}