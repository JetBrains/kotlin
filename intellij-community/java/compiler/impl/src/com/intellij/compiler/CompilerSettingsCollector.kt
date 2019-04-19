// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.internal.statistic.utils.getBooleanUsage
import com.intellij.openapi.project.Project

class CompilerSettingsCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "java.compiler.settings.project"

  override fun getUsages(project: Project): Set<UsageDescriptor> {
    val usages = mutableSetOf<UsageDescriptor>()

    val config = CompilerWorkspaceConfiguration.getInstance(project)

    usages.add(getBooleanUsage("AUTO_SHOW_ERRORS_IN_EDITOR", config.AUTO_SHOW_ERRORS_IN_EDITOR))
    usages.add(getBooleanUsage("DISPLAY_NOTIFICATION_POPUP", config.DISPLAY_NOTIFICATION_POPUP))
    usages.add(getBooleanUsage("CLEAR_OUTPUT_DIRECTORY", config.CLEAR_OUTPUT_DIRECTORY))
    usages.add(getBooleanUsage("MAKE_PROJECT_ON_SAVE", config.MAKE_PROJECT_ON_SAVE))
    usages.add(getBooleanUsage("PARALLEL_COMPILATION", config.PARALLEL_COMPILATION))
    usages.add(getBooleanUsage("REBUILD_ON_DEPENDENCY_CHANGE", config.REBUILD_ON_DEPENDENCY_CHANGE))
    usages.add(getBooleanUsage("COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT", config.COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT))

    return usages
  }
}
