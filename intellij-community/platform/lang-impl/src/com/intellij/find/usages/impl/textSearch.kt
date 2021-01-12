// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.find.usages.impl

import com.intellij.find.usages.SearchTarget
import com.intellij.find.usages.SymbolTextSearcher
import com.intellij.model.Symbol
import com.intellij.model.psi.impl.allReferencesInElement
import com.intellij.model.search.SearchContext
import com.intellij.model.search.SearchService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClassExtension
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.walkUp
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.Query
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus

private val TEXT_SEARCHERS = ClassExtension<SymbolTextSearcher<*>>("com.intellij.lang.symbolTextSearcher")

internal fun Symbol.getTextSearchStrings(): Collection<String> {
  @Suppress("UNCHECKED_CAST")
  val searchers = TEXT_SEARCHERS.forKey(javaClass) as Collection<SymbolTextSearcher<Symbol>>
  if (searchers.isEmpty()) {
    return emptyList()
  }
  val result = SmartList<String>()
  for (textSearcher in searchers) {
    result += textSearcher.getStringsToSearch(this)
  }
  return result
}

internal fun SearchTarget.hasTextSearchStrings(): Boolean = textSearchStrings.isNotEmpty()

internal fun buildTextQuery(project: Project, searchString: String, searchScope: SearchScope): Query<out Usage> {
  val length = searchString.length
  return SearchService.getInstance()
    .searchWord(project, searchString)
    .inContexts(SearchContext.IN_PLAIN_TEXT)
    .inScope(searchScope)
    .buildQuery { _, start, offsetInStart ->
      if (hasReferences(start, offsetInStart)) {
        emptyList()
      }
      else {
        listOf(UsageInfo2UsageAdapter(UsageInfo(start, offsetInStart, offsetInStart + length, true)))
      }
    }
}

private fun hasReferences(start: PsiElement, offsetInStart: Int): Boolean {
  for ((element, offsetInElement) in walkUp(start, offsetInStart)) {
    if (allReferencesInElement(element, offsetInElement).any()) {
      return true
    }
  }
  return false
}
