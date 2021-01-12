// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolService
import com.intellij.model.search.PsiSymbolDeclarationSearchParameters
import com.intellij.model.search.PsiSymbolDeclarationSearcher
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTarget
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.util.ArrayQuery
import com.intellij.util.Query

class PsiElementDeclarationSearcher : PsiSymbolDeclarationSearcher {

  override fun collectSearchRequests(parameters: PsiSymbolDeclarationSearchParameters): Collection<Query<out PsiSymbolDeclaration>> {
    val psi = PsiSymbolService.getInstance().extractElementFromSymbol(parameters.symbol) ?: return emptyList()
    val declaration = getDeclaration(psi, parameters.searchScope) ?: return emptyList()
    return listOf(ArrayQuery(declaration))
  }

  private fun getDeclaration(psi: PsiElement, searchScope: SearchScope): PsiSymbolDeclaration? {
    if (searchScope is LocalSearchScope) {
      return inLocalScope(psi, searchScope)
    }
    else {
      return inGlobalScope(psi, searchScope as GlobalSearchScope)
    }
  }

  private fun inLocalScope(psi: PsiElement, searchScope: LocalSearchScope): PsiSymbolDeclaration? {
    for (scopeElement in searchScope.scope) {
      val scopeFile = scopeElement.containingFile ?: continue
      val declarationRange = HighlightUsagesHandler.getNameIdentifierRange(scopeFile, psi) ?: continue // call old implementation as is
      return PsiElement2Declaration(psi, scopeFile, declarationRange)
    }
    return null
  }

  private fun inGlobalScope(psi: PsiElement, searchScope: GlobalSearchScope): PsiSymbolDeclaration? {
    val containingFile = psi.containingFile ?: return null
    val virtualFile = containingFile.virtualFile ?: return null
    if (!searchScope.contains(virtualFile)) {
      return null
    }
    return when (psi) {
      is PsiFile -> null // files don't have declarations inside PSI
      is PomTargetPsiElement -> fromPomTargetElement(psi)
      else -> PsiElement2Declaration.createFromTargetPsiElement(psi)
    }
  }

  private fun fromPomTargetElement(psi: PomTargetPsiElement): PsiSymbolDeclaration? {
    val target = psi.target as? PsiTarget ?: return null
    val navigationElement = target.navigationElement
    return PsiElement2Declaration.createFromPom(target, navigationElement)
  }
}
