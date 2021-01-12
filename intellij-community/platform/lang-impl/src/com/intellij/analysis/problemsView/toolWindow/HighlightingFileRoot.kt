// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.vfs.VirtualFile

internal class HighlightingFileRoot(panel: ProblemsViewPanel, val file: VirtualFile)
  : Root(panel, ProblemFilter(panel.state)) {

  private val watcher = HighlightingWatcher(this, file, HighlightSeverity.INFORMATION.myVal + 1)

  fun findProblemNode(highlighter: RangeHighlighterEx): ProblemNode? {
    val problem = watcher.findProblem(highlighter) ?: return null
    return super.findProblemNode(file, problem)
  }

  override fun addProblems(file: VirtualFile, vararg problems: Problem) {
    super.addProblems(file, *problems)
    if (problems.any { it.severity >= HighlightSeverity.ERROR.myVal }) {
      getProjectErrors()?.problemsAppeared(file)
    }
  }

  private fun getProjectErrors() = ProblemsView.getToolWindow(project)
    ?.contentManagerIfCreated
    ?.contents
    ?.mapNotNull { it.component as? ProjectErrorsPanel }
    ?.firstOrNull()
}
