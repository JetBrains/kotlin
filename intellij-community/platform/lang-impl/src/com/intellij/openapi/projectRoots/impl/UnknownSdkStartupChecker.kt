// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.StartupActivity

internal class UnknownSdkStartupChecker : StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
    checkUnknownSdks(project)

    project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object: ModuleRootListener {
      override fun rootsChanged(event: ModuleRootEvent) {
        checkUnknownSdks(event.project)
      }
    })
  }

  private fun checkUnknownSdks(project: Project) {
    UnknownSdkTracker.getInstance(project).updateUnknownSdks()
  }
}
