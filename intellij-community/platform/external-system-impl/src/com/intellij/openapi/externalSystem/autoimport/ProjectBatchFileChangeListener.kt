// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.ide.file.BatchFileChangeListener
import com.intellij.openapi.project.Project

abstract class ProjectBatchFileChangeListener(private val project: Project) : BatchFileChangeListener {
  open fun batchChangeStarted(activityName: String?) {}

  open fun batchChangeCompleted() {}

  final override fun batchChangeStarted(project: Project, activityName: String?) {
    if (project === this.project) {
      batchChangeStarted(activityName)
    }
  }

  final override fun batchChangeCompleted(project: Project) {
    if (project === this.project) {
      batchChangeCompleted()
    }
  }
}