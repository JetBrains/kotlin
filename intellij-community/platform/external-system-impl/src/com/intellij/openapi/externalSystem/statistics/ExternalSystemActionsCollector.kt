// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.execution.Executor
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project

class ExternalSystemActionsCollector {
  enum class ActionId {
    RunExternalSystemTaskAction,
    ExecuteExternalSystemRunConfigurationAction
  }

  companion object {
    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                actionId: ActionId,
                place: String?,
                isFromContextMenu: Boolean,
                executor : Executor? = null) {
      val data = FeatureUsageData().addOS().addProject(project)

      if (place != null) {
        data.addPlace(place).addData("context_menu", isFromContextMenu)
      }
      executor?.let { data.addExecutor(it) }

      addSystemId(data, systemId)

      FUCounterUsageLogger.getInstance().logEvent("build.tools.actions", actionId.name, data)
    }

    private fun addSystemId(data: FeatureUsageData,
                            systemId: ProjectSystemId?) {
      data.addData("system_id", systemId?.let { getAnonymizedSystemId(it) } ?: "undefined.system")
    }

    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                action: AnAction,
                event: AnActionEvent) {
      ActionsCollectorImpl.record("build.tools.actions", project, action, event) { data -> addSystemId(data, systemId) }
    }

    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                action: ActionId,
                event: AnActionEvent,
                executor : Executor? = null) {
      trigger(project, systemId, action, event.place, event.isFromContextMenu, executor)
    }
  }
}
