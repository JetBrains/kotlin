// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.tree.LeafState
import com.intellij.util.DocumentUtil.isValidOffset

internal class ProblemNode(parent: FileNode, val problem: Problem) : Node(parent) {

  val file = parent.file

  var offset: Int = problem.offset
    private set

  var severity: Int = problem.severity
    private set

  var description: String = problem.description
    private set

  override fun getLeafState() = LeafState.ALWAYS

  override fun getName() = description

  override fun update(project: Project, presentation: PresentationData) {
    // update values before comparison because of general contract
    offset = problem.offset
    severity = problem.severity
    description = problem.description
    presentation.addText(description, REGULAR_ATTRIBUTES)
    presentation.setIcon(problem.icon)
    val document = ProblemsView.getDocument(project, file) ?: return // add nothing if no document
    if (!isValidOffset(offset, document)) return
    val line = document.getLineNumber(offset) + 1
    presentation.addText(" :$line", GRAYED_ATTRIBUTES)
  }
}
