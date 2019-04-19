// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version

class ExternalSystemUsagesCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "build.tools"

  override fun getUsages(project: Project): Set<UsageDescriptor> {
    val usages = mutableSetOf<UsageDescriptor>()
    for (manager in ExternalSystemApiUtil.getAllManagers()) {
      if (!manager.getSettingsProvider().`fun`(project).getLinkedProjectsSettings().isEmpty()) {
        usages.add(UsageDescriptor(getAnonymizedSystemId(manager.getSystemId())))
      }
    }

    ModuleManager.getInstance(project).modules.find { ExternalSystemModulePropertyManager.getInstance(it).isMavenized() }?.let {
      usages.add(UsageDescriptor("Maven"))
    }
    return usages
  }

  companion object {
    fun getJRETypeUsage(key: String, jreName: String?): UsageDescriptor {
      val anonymizedName = when {
        jreName.isNullOrBlank() -> "empty"
        jreName in listOf(ExternalSystemJdkUtil.USE_INTERNAL_JAVA,
                          ExternalSystemJdkUtil.USE_PROJECT_JDK,
                          ExternalSystemJdkUtil.USE_JAVA_HOME) -> jreName
        else -> "custom"
      }
      return UsageDescriptor("$key.$anonymizedName", 1)
    }

    fun getJREVersionUsage(project: Project, key: String, jreName: String?): UsageDescriptor {
      val jdk = ExternalSystemJdkUtil.getJdk(project, jreName)
      val versionString =
        jdk?.versionString?.let { Version.parseVersion(it)?.let { parsed -> "${parsed.major}.${parsed.minor}" } }
        ?: "unknown"

      return UsageDescriptor("$key.$versionString", 1)
    }
  }
}
