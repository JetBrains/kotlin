// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass
import com.intellij.codeInsight.intention.impl.CachedIntentions
import com.intellij.codeInsight.intention.impl.IntentionListStep
import com.intellij.openapi.actionSystem.ActionPlaces.isMainMenuOrShortcut
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE
import com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.tree.TreeUtil.getLastUserObject
import java.awt.event.MouseEvent
import javax.swing.JTree

internal class ShowQuickFixesAction : AnAction() {

  override fun update(event: AnActionEvent) {
    val problem = getProblem(getTree(event))
    event.presentation.isEnabled = when (problem) {
      is HighlightingProblem -> isEnabled(event, problem)
      else -> false
    }
  }

  override fun actionPerformed(event: AnActionEvent) {
    when (val problem = getProblem(getTree(event))) {
      is HighlightingProblem -> actionPerformed(event, problem)
    }
  }


  private fun getTree(event: AnActionEvent) = when {
    isMainMenuOrShortcut(event.place) -> event.getData(CONTEXT_COMPONENT) as? JTree
    else -> ProblemsView.getSelectedPanel(event.project)?.tree
  }

  private fun getProblem(tree: JTree?) = tree?.let {
    getLastUserObject(ProblemNode::class.java, it.selectionPath)?.problem
  }

  private fun getPsiFile(event: AnActionEvent) = event.getData(PSI_FILE)

  private fun getEditor(psi: PsiFile) = ProblemsView.getSelectedPanel(psi.project)?.preview?.findEditor(psi)

  private fun show(event: AnActionEvent, popup: JBPopup) {
    val mouse = event.inputEvent as? MouseEvent ?: return popup.showInBestPositionFor(event.dataContext)
    val point = mouse.locationOnScreen
    val panel = ProblemsView.getSelectedPanel(event.project)
    val button = mouse.source as? ActionButton
    if (panel != null && button != null) {
      point.translate(-mouse.x, -mouse.y)
      when (panel.isVertical) {
        true -> point.x += button.width
        else -> point.x += button.height
      }
    }
    popup.show(RelativePoint.fromScreen(point))
  }


  private fun isEnabled(event: AnActionEvent, problem: HighlightingProblem): Boolean {
    val psi = getPsiFile(event) ?: return false
    getEditor(psi) ?: return false
    val markers = problem.info?.quickFixActionMarkers ?: return false
    return markers.stream().anyMatch { it.second.isValid }
  }

  private fun actionPerformed(event: AnActionEvent, problem: HighlightingProblem) {
    val psi = getPsiFile(event) ?: return
    val editor = getEditor(psi) ?: return
    val markers = problem.info?.quickFixActionMarkers ?: return

    val info = ShowIntentionsPass.IntentionsInfo()
    markers.filter { it.second.isValid }.forEach { info.intentionsToShow.add(it.first) }
    editor.caretModel.moveToOffset(problem.offset)
    info.offset = problem.offset

    val intentions = CachedIntentions.createAndUpdateActions(psi.project, psi, editor, info)
    val step = IntentionListStep(null, editor, psi, psi.project, intentions)
    show(event, JBPopupFactory.getInstance().createListPopup(step))
  }
}
