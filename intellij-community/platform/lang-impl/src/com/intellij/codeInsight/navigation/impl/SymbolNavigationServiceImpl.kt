// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.impl

import com.intellij.codeInsight.navigation.PsiElementNavigationTarget
import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolService
import com.intellij.navigation.NavigatableSymbol
import com.intellij.navigation.NavigationTarget
import com.intellij.navigation.SymbolNavigationProvider
import com.intellij.navigation.SymbolNavigationService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClassExtension
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class SymbolNavigationServiceImpl : SymbolNavigationService {

  private val ourExtension = ClassExtension<SymbolNavigationProvider>("com.intellij.symbolNavigation")

  override fun getNavigationTargets(project: Project, symbol: Symbol): Collection<NavigationTarget> {
    val result = SmartList<NavigationTarget>()
    for (provider: SymbolNavigationProvider in ourExtension.forKey(symbol.javaClass)) {
      result += provider.getNavigationTargets(project, symbol)
    }
    if (symbol is NavigatableSymbol) {
      result += symbol.getNavigationTargets(project)
    }
    val element = PsiSymbolService.getInstance().extractElementFromSymbol(symbol)
    if (element != null) {
      result += PsiElementNavigationTarget(element)
    }
    return result
  }
}
