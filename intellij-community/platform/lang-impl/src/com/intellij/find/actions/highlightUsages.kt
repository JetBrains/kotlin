// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.find.actions

import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPass
import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.injected.editor.EditorWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.model.Symbol
import com.intellij.model.psi.impl.targetSymbols
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Couple
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus

internal fun highlightUsages(project: Project, editor: Editor, file: PsiFile): Boolean {
  val allTargets = allTargets(
    project,
    targetSymbols(file, editor.caretModel.offset),
    HighlightUsagesHandler.getUsageTargets(editor, file)
  )
  if (allTargets.isEmpty()) {
    return false
  }
  val clearHighlights = HighlightUsagesHandler.isClearHighlights(editor)
  for (symbolOrTarget in allTargets) {
    when (symbolOrTarget) {
      is SymbolOrTarget.S -> highlightSymbolUsages(project, editor, file, symbolOrTarget.symbol, clearHighlights)
      is SymbolOrTarget.UT -> symbolOrTarget.target.highlightUsages(file, editor, clearHighlights)
    }
  }
  return true
}

private fun highlightSymbolUsages(project: Project, editor: Editor, file: PsiFile, symbol: Symbol, clearHighlights: Boolean) {
  val fileToUse = InjectedLanguageManager.getInstance(project).getTopLevelFile((file as? PsiCompiledFile)?.decompiledPsiFile ?: file)
  val editorToUse = (editor as? EditorWindow)?.delegate ?: editor
  val usages: Couple<List<TextRange>> = IdentifierHighlighterPass.getUsages(fileToUse, symbol)
  HighlightUsagesHandler.highlightUsages(project, editorToUse, usages, clearHighlights)
}
