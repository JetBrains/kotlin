// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolDeclarationProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.pom.PomDeclarationSearcher
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList

class DefaultPsiSymbolDeclarationProvider : PsiSymbolDeclarationProvider {

  override fun getDeclarations(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolDeclaration> {
    if (element is PsiSymbolDeclaration) {
      // PsiElement is a declaration already
      return listOf(element)
    }

    for (searcher in PomDeclarationSearcher.EP_NAME.extensions) {
      ProgressManager.checkCanceled()
      val result: MutableList<PsiSymbolDeclaration> = SmartList()
      searcher.findDeclarationsAt(element, offsetInElement, fun(target: PomTarget) {
        ProgressManager.checkCanceled()
        result += PsiElement2Declaration.createFromPom(target, element) ?: return
      })
      if (result.isNotEmpty()) {
        return listOf(result.first())
      }
    }

    return emptyList()
  }
}
