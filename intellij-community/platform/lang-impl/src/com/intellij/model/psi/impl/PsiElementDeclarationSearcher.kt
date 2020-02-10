// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolService
import com.intellij.model.search.PsiSymbolDeclarationSearchParameters
import com.intellij.model.search.PsiSymbolDeclarationSearcher
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTarget
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
    return when (psi) {
      is PsiFile -> null // files don't have declarations inside PSI
      is PomTargetPsiElement -> fromPomTargetElement(psi, searchScope)
      else -> fromPsiElement(psi, searchScope)
    }
  }

  private fun fromPomTargetElement(psi: PomTargetPsiElement, searchScope: SearchScope): PsiSymbolDeclaration? {
    val target = psi.target as? PsiTarget ?: return null
    val navigationElement = target.navigationElement
    if (navigationElement in searchScope) {
      return PsiElement2Declaration.createFromPom(target, navigationElement)
    }
    else {
      return null
    }
  }

  private fun fromPsiElement(psi: PsiElement, searchScope: SearchScope): PsiSymbolDeclaration? {
    if (psi in searchScope) {
      return PsiElement2Declaration.createFromPsi(psi, psi)
    }
    else {
      return null
    }
  }

  private operator fun SearchScope.contains(psi: PsiElement): Boolean {
    val containingFile = psi.containingFile
                         ?: return false
    if (this is LocalSearchScope) {
      val range = psi.navigationElement.textRange
                  ?: return false
      if (containsRange(containingFile, range)) {
        return true
      }
      if (isIgnoreInjectedPsi) {
        return false
      }
      val host = InjectedLanguageManager.getInstance(psi.project).getInjectionHost(containingFile) ?: return false
      return host in this
    }
    else {
      val virtualFile = containingFile.virtualFile
                        ?: return false
      return contains(virtualFile)
    }
  }
}
