// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.navigation.GotoDeclarationProvider
import com.intellij.codeInsight.navigation.PsiElementNavigationTarget
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction.findTargetElementsFromProviders
import com.intellij.model.Symbol
import com.intellij.model.SymbolReference
import com.intellij.model.psi.PsiElementSymbol
import com.intellij.navigation.NavigationTarget
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.function.Consumer

class DefaultGotoDeclarationProvider : GotoDeclarationProvider {

  override fun collectTargets(project: Project, editor: Editor, file: PsiFile, consumer: Consumer<in NavigationTarget>) {
    val elements = findTargetElementsFromProviders(project, editor, editor.caretModel.offset)
    if (elements != null && elements.isNotEmpty()) {
      for (element in elements) {
        consumer.accept(PsiElementNavigationTarget(element))
      }
      return
    }
    for (reference in findReferences(editor, file)) {
      for (resolveResult in reference.resolveReference()) {
        collectTargets(resolveResult.target, consumer)
      }
    }
  }

  private fun findReferences(editor: Editor, file: PsiFile): Collection<SymbolReference> {
    // TODO API for obtaining new SymbolReferences which are not PsiReferences
    val offset = TargetElementUtil.adjustOffset(file, editor.document, editor.caretModel.offset)
    val reference = file.findReferenceAt(offset)
    return if (reference == null) emptyList() else listOf(reference)
  }

  private fun collectTargets(symbol: Symbol, consumer: Consumer<in NavigationTarget>) {
    when (symbol) {
      is PsiElement -> consumer.accept(PsiElementNavigationTarget(symbol))
      is PsiElementSymbol -> consumer.accept(PsiElementNavigationTarget(symbol.element))
      // TODO API for obtaining NavigationTargets from new Symbol implementations which are not PsiElements
    }
  }
}
