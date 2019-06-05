// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newMetric
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version

class ExternalSystemUsagesCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "build.tools"

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val usages = mutableSetOf<MetricEvent>()
    for (manager in ExternalSystemApiUtil.getAllManagers()) {
      if (!manager.getSettingsProvider().`fun`(project).getLinkedProjectsSettings().isEmpty()) {
        usages.add(newMetric("externalSystemId", getAnonymizedSystemId(manager.getSystemId())))
      }
    }

    ModuleManager.getInstance(project).modules.find { ExternalSystemModulePropertyManager.getInstance(it).isMavenized() }?.let {
      usages.add(newMetric("externalSystemId", "Maven"))
    }
    return usages
  }

  companion object {
    fun getJRETypeUsage(key: String, jreName: String?): MetricEvent {
      val anonymizedName = when {
        jreName.isNullOrBlank() -> "empty"
        jreName in listOf(ExternalSystemJdkUtil.USE_INTERNAL_JAVA,
                          ExternalSystemJdkUtil.USE_PROJECT_JDK,
                          ExternalSystemJdkUtil.USE_JAVA_HOME) -> jreName
        else -> "custom"
      }
      return newMetric(key, anonymizedName)
    }

    fun getJREVersionUsage(project: Project, key: String, jreName: String?): MetricEvent {
      val jdk = ExternalSystemJdkUtil.getJdk(project, jreName)
      val versionString =
        jdk?.versionString?.let { Version.parseVersion(it)?.let { parsed -> "${parsed.major}.${parsed.minor}" } }
        ?: "unknown"

      return newMetric(key, versionString)
    }
  }
}
