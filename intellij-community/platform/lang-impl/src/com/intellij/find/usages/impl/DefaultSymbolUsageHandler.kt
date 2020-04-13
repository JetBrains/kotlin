// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages.impl

import com.intellij.find.FindBundle
import com.intellij.find.usages.NonConfigurableUsageHandler
import com.intellij.find.usages.UsageOptions
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.search.SearchService
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.EmptyQuery
import com.intellij.util.Query
import com.intellij.util.mapNotNull

internal class DefaultSymbolUsageHandler(
  private val project: Project,
  private val symbol: Symbol
) : NonConfigurableUsageHandler() {

  override fun getMaximalSearchScope(): SearchScope = GlobalSearchScope.allScope(project)

  override fun getSearchString(options: UsageOptions): String {
    val shortNameString = SymbolPresentationService.getInstance().getSymbolPresentation(symbol).shortNameString
    return FindBundle.message("usages.search.title.default", shortNameString)
  }

  override fun buildSearchQuery(options: UsageOptions): Query<out Usage> {
    if (options.isUsages) {
      return SearchService.getInstance()
        .searchPsiSymbolReferences(project, symbol, options.searchScope)
        .mapNotNull(::createUsage)
    }
    else {
      return EmptyQuery.getEmptyQuery()
    }
  }

  private fun createUsage(reference: PsiSymbolReference): Usage? {
    val rangeInElement = reference.rangeInElement
    val usageInfo = UsageInfo(reference.element, rangeInElement.startOffset, rangeInElement.endOffset, false)
    return UsageInfo2UsageAdapter(usageInfo)
  }
}
