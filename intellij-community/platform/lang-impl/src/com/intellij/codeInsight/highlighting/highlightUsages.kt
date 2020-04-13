// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.codeInsight.highlighting

import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPass
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
  val allTargets = targetSymbols(file, editor.caretModel.offset)
  if (allTargets.isEmpty()) {
    return false
  }
  val clearHighlights = HighlightUsagesHandler.isClearHighlights(editor)
  for (symbol in allTargets) {
    highlightSymbolUsages(project, editor, file, symbol, clearHighlights)
  }
  return true
}

private fun highlightSymbolUsages(project: Project, editor: Editor, file: PsiFile, symbol: Symbol, clearHighlights: Boolean) {
  val fileToUse = InjectedLanguageManager.getInstance(project).getTopLevelFile((file as? PsiCompiledFile)?.decompiledPsiFile ?: file)
  val editorToUse = (editor as? EditorWindow)?.delegate ?: editor
  val usages: Couple<List<TextRange>> = IdentifierHighlighterPass.getUsages(fileToUse, symbol)
  HighlightUsagesHandler.highlightUsages(project, editorToUse, usages, clearHighlights)
}
