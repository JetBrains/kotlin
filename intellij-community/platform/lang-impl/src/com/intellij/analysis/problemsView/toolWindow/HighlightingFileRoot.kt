// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile

internal class HighlightingFileRoot(panel: ProblemsViewPanel, val file: VirtualFile) : Root(panel) {

  private val watcher = HighlightingWatcher(this, file, INFORMATION.myVal + 1)

  init {
    Disposer.register(this, watcher)
  }

  fun findProblemNode(highlighter: RangeHighlighterEx): ProblemNode? {
    val problem = watcher.findProblem(highlighter) ?: return null
    return super.findProblemNode(file, problem)
  }
}
