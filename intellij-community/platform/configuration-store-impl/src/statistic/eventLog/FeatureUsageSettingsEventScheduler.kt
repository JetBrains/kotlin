// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.statistic.eventLog

import com.intellij.concurrency.JobScheduler
import com.intellij.configurationStore.ComponentStoreImpl
import com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEvents.logConfigurationState
import com.intellij.internal.statistic.eventLog.fus.FeatureUsageLogger
import com.intellij.internal.statistic.eventLog.fus.FeatureUsageStateEventTracker
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.util.ArrayUtilRt
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

private val LOG = logger<FeatureUsageSettingsEventScheduler>()

private const val PERIOD_DELAY = 24 * 60
private const val INITIAL_DELAY = PERIOD_DELAY

internal class FeatureUsageSettingsEventScheduler : FeatureUsageStateEventTracker {
  override fun initialize() {
    if (!FeatureUsageLogger.isEnabled()) {
      return
    }

    JobScheduler.getScheduler().scheduleWithFixedDelay(
      { logConfigStateEvents() },
      INITIAL_DELAY.toLong(), PERIOD_DELAY.toLong(), TimeUnit.MINUTES
    )
  }

  private fun logConfigStateEvents() {
    if (!FeatureUsageLogger.isEnabled()) {
      return
    }

    logInitializedComponents(ApplicationManager.getApplication())

    val projectManager = ProjectManagerEx.getInstanceEx()
    val projects = ArrayDeque(projectManager.openProjects.toList())
    if (projectManager.isDefaultProjectInitialized) {
      projects.addFirst(projectManager.defaultProject)
    }
    logProjectInitializedComponentsAndContinue(projects)
  }

  private fun logProjectInitializedComponentsAndContinue(projects: ArrayDeque<Project>): CompletableFuture<Void?> {
    val project = projects.pollFirst()
    if (project == null || !project.isInitialized || project.isDisposed) {
      return CompletableFuture.completedFuture(null)
    }
    else {
      return logInitializedComponents(project)
        .thenCompose {
          logProjectInitializedComponentsAndContinue(projects)
        }
    }
  }

  private fun logInitializedComponents(componentManager: ComponentManager): CompletableFuture<Void?> {
    val stateStore = (componentManager.stateStore as? ComponentStoreImpl) ?: return CompletableFuture.completedFuture(null)
    return CompletableFuture.runAsync(Runnable {
      val components = stateStore.getComponents()
      for (name in ArrayUtilRt.toStringArray(components.keys)) {
        val info = components[name]
        val component = info?.component ?: continue
        try {
          if (component is PersistentStateComponent<*>) {
            val stateSpec = info.stateSpec ?: continue
            val componentState = component.state ?: continue
            logConfigurationState(name, stateSpec, componentState, componentManager as? Project)
          }
        }
        catch (e: Exception) {
          LOG.warn("Error during configuration recording", e)
        }
      }
    }, Executor { ApplicationManager.getApplication().invokeLater(it) })
  }
}

