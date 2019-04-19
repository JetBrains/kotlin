// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.ArrayUtilRt
import java.util.concurrent.TimeUnit

private val LOG = logger<FeatureUsageSettingsEventScheduler>()

private const val PERIOD_DELAY = 24 * 60
private const val INITIAL_DELAY = PERIOD_DELAY

class FeatureUsageSettingsEventScheduler : FeatureUsageStateEventTracker {
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
    logInitializedComponents(ProjectManager.getInstance().defaultProject)
    ProjectManager.getInstance().openProjects.filter { project -> !project.isDefault }.forEach { project ->
      logInitializedComponents(project)
    }
  }

  private fun logInitializedComponents(componentManager: ComponentManager) {
    val stateStore = (componentManager.stateStore as? ComponentStoreImpl) ?: return
    ApplicationManager.getApplication().invokeLater {
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
    }
  }
}

