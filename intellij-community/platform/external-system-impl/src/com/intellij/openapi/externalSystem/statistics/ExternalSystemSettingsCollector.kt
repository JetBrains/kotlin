// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.internal.statistic.utils.getBooleanUsage
import com.intellij.internal.statistic.utils.getCountingUsage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project

class ExternalSystemSettingsCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "build.tools.state"

  override fun getUsages(project: Project): Set<UsageDescriptor> {
    val usages = mutableSetOf<UsageDescriptor>()

    for (manager in ExternalSystemApiUtil.getAllManagers()) {
      val systemId = getAnonymizedSystemId(manager.getSystemId())
      fun addWithSustemId(desc: UsageDescriptor) {
        desc.data.addData("externalSystemId", systemId)
        usages.add(desc)
      }

      val projects = manager.getSettingsProvider().`fun`(project).getLinkedProjectsSettings()

      addWithSustemId(getCountingUsage("numberOfLinkedProject", projects.size))

      for (projectsSetting in projects) {
        addWithSustemId(getBooleanUsage("autoImport", projectsSetting.isUseAutoImport))
        addWithSustemId(getBooleanUsage("useQualifiedModuleNames", projectsSetting.isUseQualifiedModuleNames))
        addWithSustemId(getCountingUsage("modules.count", projectsSetting.modules.size))
      }
    }

    return usages
  }
}
