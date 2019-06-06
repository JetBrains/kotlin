// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.statistics

import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.project.Project

class GradleActionsUsagesCollector {
  enum class ActionID {
    refreshDaemons,
    stopAllDaemons,
    stopSelectedDaemons,
    PasteMvnDependency,
    showGradleDaemonsAction
  }

  companion object {
    @JvmStatic
    fun trigger(project: Project?, action: ActionID) {
      if (project == null) return
      FUCounterUsageLogger.getInstance().logEvent(project, "build.gradle.actions", action.name)
    }
  }
}
