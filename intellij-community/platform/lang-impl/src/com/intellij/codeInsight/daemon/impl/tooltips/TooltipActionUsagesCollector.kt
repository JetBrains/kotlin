// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl.tooltips

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.internal.statistic.utils.getBooleanUsage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry

class TooltipActionUsagesCollector : ProjectUsagesCollector() {
  override fun getGroupId(): String = "tooltip.actions"

  override fun getUsages(project: Project): Set<UsageDescriptor> {
    if (!Registry.`is`("ide.tooltip.show.with.actions")) return emptySet()
    
    return setOf(getBooleanUsage("panel.show.value", TooltipActionProvider.isShowActions()))
  }
} 
