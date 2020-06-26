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
        true -> point.y += button.height
        else -> point.x += button.width
      }
    }
    popup.show(RelativePoint.fromScreen(point))
  }


  private fun isEnabled(event: AnActionEvent, problem: HighlightingProblem): Boolean {
    return getCachedIntentions(event, problem) != null
  }

  private fun actionPerformed(event: AnActionEvent, problem: HighlightingProblem) {
    val intentions = getCachedIntentions(event, problem) ?: return
    val editor = intentions.editor ?: return
    editor.caretModel.moveToOffset(intentions.offset)
    show(event, JBPopupFactory.getInstance().createListPopup(
      IntentionListStep(null, editor, intentions.file, intentions.file.project, intentions)
    ))
  }

  private fun getCachedIntentions(event: AnActionEvent, problem: HighlightingProblem): CachedIntentions? {
    val psi = getPsiFile(event) ?: return null
    val editor = getEditor(psi) ?: return null
    val markers = problem.info?.quickFixActionMarkers ?: return null

    val info = ShowIntentionsPass.IntentionsInfo()
    markers.filter { it.second.isValid }.forEach { info.intentionsToShow.add(it.first) }
    info.offset = problem.offset

    val intentions = CachedIntentions.createAndUpdateActions(psi.project, psi, editor, info)
    if (intentions.intentions.isNotEmpty()) return intentions
    return null // actions can be removed after updating
  }
}
