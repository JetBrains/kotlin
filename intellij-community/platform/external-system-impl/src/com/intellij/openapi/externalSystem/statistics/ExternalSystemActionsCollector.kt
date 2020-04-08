// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.execution.Executor
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsEventLogGroup
import com.intellij.internal.statistic.eventLog.EventFields
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.service.fus.collectors.FeatureUsagesCollector
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project

class ExternalSystemActionsCollector : CounterUsagesCollector() {
  enum class ActionId {
    RunExternalSystemTaskAction,
    ExecuteExternalSystemRunConfigurationAction
  }

  override fun getGroup(): EventLogGroup = GROUP

  companion object {
    private val GROUP = EventLogGroup("build.tools.actions", 2)
    private val EXTERNAL_SYSTEM_ID = EventFields.String("system_id").withCustomEnum("build_tools")
    private val ACTION_INVOKED = ActionsEventLogGroup.registerActionInvokedEvent(GROUP, "action.invoked", EXTERNAL_SYSTEM_ID)

    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                actionId: ActionId,
                place: String?,
                isFromContextMenu: Boolean,
                executor : Executor? = null) {
      val data = FeatureUsageData().addProject(project)

      if (place != null) {
        data.addPlace(place).addData("context_menu", isFromContextMenu)
      }
      executor?.let { data.addData("executor", it.id) }

      addExternalSystemId(data, systemId)

      FUCounterUsageLogger.getInstance().logEvent("build.tools.actions", actionId.name, data)
    }

    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                action: AnAction,
                event: AnActionEvent) {
      ActionsCollectorImpl.record(ACTION_INVOKED, project, action, event, listOf(EXTERNAL_SYSTEM_ID with anonymizeSystemId(systemId)))
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
