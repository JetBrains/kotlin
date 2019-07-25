// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction.*
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.util.containers.toArray

/**
 * Go To Declaration Handler which doesn't invoke Show Usages if there are no declarations to go
 */
object GotoDeclarationOnlyHandler : CodeInsightActionHandler {

  override fun startInWriteAction(): Boolean = false

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration.only")
    val dumbService = DumbService.getInstance(project)
    try {
      dumbService.isAlternativeResolveEnabled = true
      val offset = editor.caretModel.offset
      val elements = underModalProgress(project, "Resolving Reference...") {
        findAllTargetElements(project, editor, offset)
      }
      if (elements.size == 1) {
        // simplest case
        val element = elements[0]
        val navElement = TargetElementUtil.getInstance().getGotoDeclarationTarget(element, element.navigationElement)
        if (navElement != null) {
          gotoTargetElement(navElement, editor, file)
        }
      }
      else if (elements.isEmpty()) {
        // this means either there is really nowhere to go or weird TargetElementUtil didn't return anything
        val reference = TargetElementUtil.findReference(editor, offset)
        if (reference != null) {
          val targets = underModalProgress(project, "Resolving Reference...") {
            TargetElementUtil.getInstance().getTargetCandidates(reference)
          }
          if (targets.isNotEmpty()) {
            chooseAmbiguousTarget(editor, file, targets.toArray(PsiElement.EMPTY_ARRAY))
            return
          }
        }
        //disable 'no declaration found' notification for keywords
        if (!isKeywordUnderCaret(project, file, offset)) {
          HintManager.getInstance().showErrorHint(editor, "Cannot find declaration to go to")
        }
      }
      else {
        chooseAmbiguousTarget(editor, file, elements)
      }
    }
    catch (e: IndexNotReadyException) {
      dumbService.showDumbModeNotification("Navigation is not available here during index update")
    }
    finally {
      dumbService.isAlternativeResolveEnabled = false
    }
  }

  private fun chooseAmbiguousTarget(editor: Editor, file: PsiFile, elements: Array<PsiElement>) {
    if (!editor.component.isShowing) return
    chooseAmbiguousTarget(editor, CodeInsightBundle.message("declaration.navigation.title"), elements) { element ->
      gotoTargetElement(element, editor, file)
    }
  }

  private fun chooseAmbiguousTarget(editor: Editor, title: String, elements: Array<PsiElement>, processor: (PsiElement) -> Unit) {
    require(elements.isNotEmpty())
    if (elements.size == 1) {
      val element = elements[0]
      processor(element)
      return
    }
    val psiElementProcessor = PsiElementProcessor<PsiElement> {
      processor(it)
      true
    }
    NavigationUtil.getPsiElementPopup(elements, DefaultPsiElementCellRenderer(), title, psiElementProcessor).showInBestPositionFor(editor)
  }
}
