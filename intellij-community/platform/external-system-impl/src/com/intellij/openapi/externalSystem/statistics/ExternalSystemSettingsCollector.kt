// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newBooleanMetric
import com.intellij.internal.statistic.beans.newCounterMetric
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project

class ExternalSystemSettingsCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "build.tools.state"

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val usages = mutableSetOf<MetricEvent>()

    for (manager in ExternalSystemApiUtil.getAllManagers()) {
      val systemId = getAnonymizedSystemId(manager.getSystemId())
      fun addWithSystemId(desc: MetricEvent) {
        desc.data.addData("externalSystemId", systemId)
        usages.add(desc)
      }

      val projects = manager.getSettingsProvider().`fun`(project).getLinkedProjectsSettings()

      addWithSystemId(newCounterMetric("numberOfLinkedProject", projects.size))

      for (projectsSetting in projects) {
        addWithSystemId(newBooleanMetric("autoImport", projectsSetting.isUseAutoImport))
        addWithSystemId(newBooleanMetric("useQualifiedModuleNames", projectsSetting.isUseQualifiedModuleNames))
        addWithSystemId(newCounterMetric("modules.count", projectsSetting.modules.size))
      }
    }

    return usages
  }
}
