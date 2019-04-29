// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.statistics

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.FUSUsageContext
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.internal.statistic.utils.getBooleanUsage
import com.intellij.internal.statistic.utils.getCountingUsage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project

class ExternalSystemSettingsCollector() : ProjectUsagesCollector() {
  override fun getGroupId() = "build.tools.state"

  override fun getUsages(project: Project): Set<UsageDescriptor> {
    val usages = mutableSetOf<UsageDescriptor>()

    for (manager in ExternalSystemApiUtil.getAllManagers()) {
      val context = FUSUsageContext.create(getAnonymizedSystemId(manager.getSystemId()))
      for (projectsSetting in manager.getSettingsProvider().`fun`(project).getLinkedProjectsSettings()) {
        usages.add(addContext(getBooleanUsage("autoImport", projectsSetting.isUseAutoImport), context))
        usages.add(addContext(getBooleanUsage("useQualifiedModuleNames", projectsSetting.isUseQualifiedModuleNames), context))
        usages.add(addContext(getCountingUsage("modules.count", projectsSetting.modules.size), context))
      }
    }
    return usages
  }

  companion object {
    private fun addContext(desc: UsageDescriptor, ctx: FUSUsageContext?) = UsageDescriptor(desc.key, desc.value, ctx)
  }
}
