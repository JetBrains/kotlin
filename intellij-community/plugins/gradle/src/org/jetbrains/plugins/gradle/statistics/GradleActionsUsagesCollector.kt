// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.service.fus.collectors.FUSUsageContext
import com.intellij.internal.statistic.service.fus.collectors.UsageDescriptorKeyValidator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class GradleActionsUsagesCollector {
  companion object {
    @JvmStatic
    fun trigger(project: Project?, action: AnAction, event: AnActionEvent?) {
      if (project == null) return

      // preserve context data ordering
      val context = FUSUsageContext.create(
        event?.place ?: "undefined.place",
        "fromContextMenu.${event?.isFromContextMenu?.toString() ?: "false"}"
      )

      val actionClassName = UsageDescriptorKeyValidator.ensureProperKey(action.javaClass.simpleName)

      FUCounterUsageLogger.getInstance().logEvent(project, "build.gradle.actions", actionClassName, FeatureUsageData().addFeatureContext(context))
    }

    @JvmStatic
    fun trigger(project: Project?, feature: String) {
      if (project == null) return
      FUCounterUsageLogger.getInstance().logEvent(project, "build.gradle.actions", feature)
    }
  }
}
