// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx

fun Project.use(save: Boolean = false, action: (Project) -> Unit) {
  val project = this@use
  try {
    action(project)
  }
  finally {
    invokeAndWaitIfNeeded {
      val projectManager = ProjectManagerEx.getInstanceEx()
      if (save) {
        projectManager.closeAndDispose(project)
      }
      else {
        projectManager.forceCloseProject(project, true)
      }
    }
  }
}