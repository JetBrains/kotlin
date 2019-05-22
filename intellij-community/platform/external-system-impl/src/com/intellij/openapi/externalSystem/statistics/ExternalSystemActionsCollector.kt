// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.service.fus.collectors.UsageDescriptorKeyValidator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project

class ExternalSystemActionsCollector {

  companion object {
    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                featureId: String,
                place: String?,
                isFromContextMenu: Boolean,
                vararg additionalContextData: Pair<String, String>) {
      val data = FeatureUsageData().addOS().addProject(project)

      if (place != null) {
        data.addPlace(place).addData("context_menu", isFromContextMenu)
      }
      for (each in additionalContextData) {
        data.addData(each.first, each.second)
      }
      addSystemId(data, systemId)

      FUCounterUsageLogger.getInstance().logEvent("build.tools.actions", UsageDescriptorKeyValidator.ensureProperKey(featureId), data)
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
                featureId: String,
                event: AnActionEvent,
                vararg additionalContextData: Pair<String, String>) {
      trigger(project, systemId, featureId, event.place, event.isFromContextMenu, *additionalContextData)
    }
  }
}
