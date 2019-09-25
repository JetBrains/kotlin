// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.VcsPreservingExecutor
import com.intellij.openapi.vfs.VirtualFile


fun runAnalysisAfterShelvingSync(project: Project, files: List<VirtualFile>, progressIndicator: ProgressIndicator, afterShelve: Runnable) {
  var tryNo = 100
  //TODO fix hacky hack
  while (ProjectLevelVcsManager.getInstance(project).allVcsRoots.isEmpty() && tryNo > 0)
  {
    Thread.sleep(50)
    tryNo--
  }
  val versionedRoots = files.mapNotNull { ProjectLevelVcsManager.getInstance(project).getVcsRootFor(it) }.toSet()
  val message = VcsBundle.message("searching.for.code.smells.freezing.process")
  VcsPreservingExecutor.executeOperation(project, versionedRoots, message, progressIndicator, afterShelve)
}