// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.execution.Executor
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsEventLogGroup
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsEventLogGroup.Companion.ACTION_INVOKED_EVENT_ID
import com.intellij.internal.statistic.eventLog.EventFields
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.EventPair
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
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
    private val GROUP = EventLogGroup("build.tools.actions", 3)
    private val EXTERNAL_SYSTEM_ID = EventFields.String("system_id").withCustomEnum("build_tools")
    private val ACTION_EXECUTOR_FIELD = EventFields.String("executor").withCustomRule("run_config_executor")
    private val DELEGATE_ACTION_ID = EventFields.Enum<ActionId>("action_id")

    private val ACTION_INVOKED = ActionsEventLogGroup.registerActionInvokedEvent(GROUP, ACTION_INVOKED_EVENT_ID, EXTERNAL_SYSTEM_ID)
    private val DELEGATE_ACTION_INVOKED = GROUP.registerVarargEvent(
      ACTION_INVOKED_EVENT_ID, DELEGATE_ACTION_ID, EventFields.ActionPlace,
      ActionsEventLogGroup.CONTEXT_MENU, ACTION_EXECUTOR_FIELD, EXTERNAL_SYSTEM_ID
    )

    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                actionId: ActionId,
                place: String?,
                isFromContextMenu: Boolean,
                executor : Executor? = null) {
      val data: MutableList<EventPair<*>> = arrayListOf(DELEGATE_ACTION_ID.with(actionId))

      if (place != null) {
        data.add(EventFields.ActionPlace.with(place))
        data.add(ActionsEventLogGroup.CONTEXT_MENU.with(isFromContextMenu))
      }
      executor?.let { data.add(ACTION_EXECUTOR_FIELD.with(it.id)) }

      data.add(EXTERNAL_SYSTEM_ID.with(anonymizeSystemId(systemId)))

      DELEGATE_ACTION_INVOKED.log(project, *data.toTypedArray())
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
