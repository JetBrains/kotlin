// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection
import javax.swing.JTree

internal class CopyProblemDescriptionAction : NodeAction<Problem>() {
  override fun getData(node: Any?) = (node as? ProblemNode)?.problem
  override fun actionPerformed(data: Problem) = CopyPasteManager.getInstance().setContents(StringSelection(data.description))
}

internal abstract class NodeAction<Data> : DumbAwareAction() {
  abstract fun getData(node: Any?): Data?
  abstract fun actionPerformed(data: Data)
  open fun isEnabled(data: Data) = true

  override fun update(event: AnActionEvent) {
    val data = getData(getSelectedNode(event))
    event.presentation.isEnabledAndVisible = data != null && isEnabled(data)
  }

  override fun actionPerformed(event: AnActionEvent) {
    val data = getData(getSelectedNode(event))
    if (data != null) actionPerformed(data)
  }
}

private fun getSelectedNode(event: AnActionEvent): Any? {
  val tree = event.getData(CONTEXT_COMPONENT) as? JTree
  return tree?.selectionPath?.lastPathComponent
}
