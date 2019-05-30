// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.navigation.CtrlMouseInfo
import com.intellij.codeInsight.navigation.impl.GTDActionData
import com.intellij.codeInsight.navigation.impl.GTDActionResult
import com.intellij.codeInsight.navigation.impl.fromGTDProviders
import com.intellij.codeInsight.navigation.impl.gotoDeclaration
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.navigation.TargetPopupPresentation
import com.intellij.navigation.chooseTargetPopup
import com.intellij.openapi.actionSystem.ex.ActionUtil.underModalProgress
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile

internal object GotoDeclarationOnlyHandler2 : CodeInsightActionHandler {

  override fun startInWriteAction(): Boolean = false

  private fun gotoDeclaration(project: Project, editor: Editor, file: PsiFile, offset: Int): GTDActionData? {
    return fromGTDProviders(project, editor, offset)
           ?: gotoDeclaration(file, offset)
  }

  fun getCtrlMouseInfo(editor: Editor, file: PsiFile, offset: Int): CtrlMouseInfo? {
    return gotoDeclaration(file.project, editor, file, offset)?.ctrlMouseInfo()
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration.only")
    if (navigateToLookupItem(project, editor, file)) {
      return
    }
    if (EditorUtil.isCaretInVirtualSpace(editor)) {
      return
    }

    val offset = editor.caretModel.offset
    val actionResult: GTDActionResult? = try {
      underModalProgress(project, CodeInsightBundle.message("progress.title.resolving.reference")) {
        gotoDeclaration(project, editor, file, offset)?.result()
      }
    }
    catch (e: IndexNotReadyException) {
      DumbService.getInstance(project).showDumbModeNotification("Navigation is not available here during index update")
      return
    }

    if (actionResult == null) {
      notifyNowhereToGo(project, editor, file, offset)
    }
    else {
      gotoDeclaration(editor, file, actionResult)
    }
  }

  internal fun gotoDeclaration(editor: Editor, file: PsiFile, actionResult: GTDActionResult) {
    when (actionResult) {
      is GTDActionResult.SingleTarget -> gotoTarget(editor, file, actionResult.navigatable)
      is GTDActionResult.MultipleTargets -> {
        val popup = chooseTargetPopup(
          CodeInsightBundle.message("declaration.navigation.title"),
          actionResult.targets, Pair<Navigatable, TargetPopupPresentation>::second
        ) { (navigatable, _) ->
          gotoTarget(editor, file, navigatable)
        }
        popup.showInBestPositionFor(editor)
      }
    }
  }
}
