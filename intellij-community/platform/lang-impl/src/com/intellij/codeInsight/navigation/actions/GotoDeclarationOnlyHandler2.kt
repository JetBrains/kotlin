// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.findAllTargets
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction.isKeywordUnderCaret
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction.underModalProgress
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.navigation.NavigationTarget
import com.intellij.navigation.chooseTarget
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile

object GotoDeclarationOnlyHandler2 : CodeInsightActionHandler {

  override fun startInWriteAction(): Boolean = false

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration")

    val targets = try {
      underModalProgress(project, "Resolving Reference...") {
        findAllTargets(project, editor, file)
      }
    }
    catch (e: IndexNotReadyException) {
      DumbService.getInstance(project).showDumbModeNotification("Navigation is not available here during index update")
      return
    }

    if (targets.isEmpty()) {
      notifyCantGoAnywhere(project, editor, file)
    }
    else {
      chooseTarget(editor, CodeInsightBundle.message("declaration.navigation.title"), targets.toList()) {
        gotoTarget(project, editor, file, it)
      }
    }
  }

  private fun notifyCantGoAnywhere(project: Project, editor: Editor, file: PsiFile) {
    if (!isKeywordUnderCaret(project, file, editor.caretModel.offset)) {
      HintManager.getInstance().showErrorHint(editor, "Cannot find declaration to go to")
    }
  }

  private fun gotoTarget(project: Project, editor: Editor, file: PsiFile, target: NavigationTarget) {
    val navigatable = target.navigatable ?: return
    if (navigateInCurrentEditor(project, editor, file, navigatable)) {
      return
    }
    if (navigatable.canNavigate()) {
      navigatable.navigate(true)
    }
  }

  private fun navigateInCurrentEditor(project: Project, editor: Editor, file: PsiFile, target: Navigatable): Boolean {
    if (!editor.isDisposed && target is OpenFileDescriptor && target.file == file.virtualFile) {
      executeCommand {
        IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
        target.navigateIn(editor)
      }
      return true
    }
    return false
  }
}
