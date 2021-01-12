// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.navigation.CtrlMouseInfo
import com.intellij.codeInsight.navigation.actions.GotoDeclarationOnlyHandler2.gotoDeclaration
import com.intellij.codeInsight.navigation.impl.*
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.find.FindBundle
import com.intellij.find.actions.ShowUsagesAction.showUsages
import com.intellij.find.usages.SearchTarget
import com.intellij.find.usages.impl.symbolSearchTargets
import com.intellij.model.Symbol
import com.intellij.navigation.chooseTargetPopup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil.underModalProgress
import com.intellij.openapi.actionSystem.impl.SimpleDataContext.getSimpleContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly

object GotoDeclarationOrUsageHandler2 : CodeInsightActionHandler {

  override fun startInWriteAction(): Boolean = false

  private fun gotoDeclarationOrUsages(project: Project, editor: Editor, file: PsiFile, offset: Int): GTDUActionData? {
    return fromGTDProviders(project, editor, offset)?.toGTDUActionData()
           ?: gotoDeclarationOrUsages(file, offset)
  }

  @JvmStatic
  fun getCtrlMouseInfo(editor: Editor, file: PsiFile, offset: Int): CtrlMouseInfo? {
    return gotoDeclarationOrUsages(file.project, editor, file, offset)?.ctrlMouseInfo()
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration")
    if (navigateToLookupItem(project, editor, file)) {
      return
    }
    if (EditorUtil.isCaretInVirtualSpace(editor)) {
      return
    }

    val offset = editor.caretModel.offset
    val actionResult: GTDUActionResult? = try {
      underModalProgress(project, CodeInsightBundle.message("progress.title.resolving.reference")) {
        gotoDeclarationOrUsages(project, editor, file, offset)?.result()
      }
    }
    catch (e: IndexNotReadyException) {
      DumbService.getInstance(project).showDumbModeNotification("Navigation is not available here during index update")
      return
    }

    when (actionResult) {
      null -> notifyNowhereToGo(project, editor, file, offset)
      is GTDUActionResult.GTD -> {
        GotoDeclarationAction.recordGTD()
        gotoDeclaration(editor, file, actionResult.gtdActionResult)
      }
      is GTDUActionResult.SU -> {
        GotoDeclarationAction.recordSU()
        showUsages(project, editor, file, actionResult.targets)
      }
    }
  }

  private fun showUsages(project: Project, editor: Editor, file: PsiFile, targetSymbols: List<Symbol>) {
    val searchTargets = symbolSearchTargets(project, targetSymbols)
    chooseTargetAndShowUsages(project, editor, file, searchTargets)
  }

  private fun chooseTargetAndShowUsages(project: Project, editor: Editor, file: PsiFile, searchTargets: List<SearchTarget>) {
    require(searchTargets.isNotEmpty())
    val contextMap: Map<String, Any> = mapOf(
      CommonDataKeys.PSI_FILE.name to file,
      CommonDataKeys.EDITOR.name to editor
    )
    val dataContext = getSimpleContext(contextMap, DataContext.EMPTY_CONTEXT, true)
    val singleTarget = searchTargets.singleOrNull()
    if (singleTarget != null) {
      showUsages(project, dataContext, singleTarget)
    }
    else {
      val popup = chooseTargetPopup(
        FindBundle.message("show.usages.ambiguous.title"),
        searchTargets, SearchTarget::presentation
      ) {
        showUsages(project, dataContext, it)
      }
      popup.showInBestPositionFor(dataContext)
    }
  }

  @TestOnly
  enum class GTDUOutcome {
    GTD,
    SU,
    ;
  }

  @TestOnly
  @JvmStatic
  fun testGTDUOutcome(editor: Editor, file: PsiFile, offset: Int): GTDUOutcome? {
    return when (gotoDeclarationOrUsages(file.project, editor, file, offset)?.result()) {
      null -> null
      is GTDUActionResult.GTD -> GTDUOutcome.GTD
      is GTDUActionResult.SU -> GTDUOutcome.SU
    }
  }
}
