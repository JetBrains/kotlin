// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.service.fus.collectors.FUSUsageContext
import com.intellij.internal.statistic.service.fus.collectors.UsageDescriptorKeyValidator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.util.text.nullize

class ExternalSystemActionsCollector {

  companion object {
    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                featureId: String,
                place: String?,
                isFromContextMenu: Boolean,
                vararg additionalContextData: String) {
      if (project == null) return

      // preserve context data ordering
      val context = FUSUsageContext.create(
        place.nullize(true) ?: "undefined place",
        "fromContextMenu.$isFromContextMenu",
        systemId?.let { getAnonymizedSystemId(it) } ?: "undefined.system",
        *additionalContextData
      )

      FUCounterUsageLogger.getInstance().logEvent(project, "build.tools.actions",
                                                  UsageDescriptorKeyValidator.ensureProperKey(featureId),
                                                  FeatureUsageData().addFeatureContext(context)
      )
    }


    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                action: AnAction,
                event: AnActionEvent,
                vararg additionalContextData: String) {
      trigger(project, systemId, action.javaClass.simpleName, event, *additionalContextData)
    }

    @JvmStatic
    fun trigger(project: Project?,
                systemId: ProjectSystemId?,
                featureId: String,
                event: AnActionEvent,
                vararg additionalContextData: String) {
      trigger(project, systemId, featureId, event.place, event.isFromContextMenu, *additionalContextData)
    }
  }
}
