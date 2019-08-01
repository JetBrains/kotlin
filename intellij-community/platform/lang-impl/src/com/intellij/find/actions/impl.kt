// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.find.actions

import com.intellij.find.FindSettings
import com.intellij.find.usages.SymbolUsageHandlerFactory
import com.intellij.find.usages.UsageHandler
import com.intellij.find.usages.UsageOptions
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService
import com.intellij.model.search.SearchService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClassExtension
import com.intellij.openapi.util.Factory
import com.intellij.psi.impl.search.runSearch
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewContentManager
import com.intellij.usages.Usage
import com.intellij.usages.UsageSearcher
import com.intellij.usages.UsageViewManager
import com.intellij.usages.UsageViewPresentation
import com.intellij.util.Query
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nls.Capitalization.Sentence

internal val LOG: Logger = Logger.getInstance("#com.intellij.find.actions")

private val EXTENSION = ClassExtension<SymbolUsageHandlerFactory<*>>("com.intellij.lang.symbolUsagesHandler")

internal fun Symbol.createUsageHandler(project: Project): UsageHandler<*> {
  for (factory in EXTENSION.forKey(this.javaClass)) {
    @Suppress("UNCHECKED_CAST")
    val factory_ = factory as SymbolUsageHandlerFactory<Symbol>
    val handler = factory_.createHandler(project, this)
    if (handler != null) {
      return handler
    }
  }
  return DefaultSymbolUsageHandler(project, this)
}

internal fun Symbol.presentableText(): @Nls(capitalization = Sentence) String {
  return SymbolPresentationService.getInstance().getSymbolPresentation(this).longDescription
}

fun findUsages(showDialog: Boolean, project: Project, selectedScope: SearchScope, symbol: Symbol) {
  findUsages(showDialog, project, symbol, symbol.createUsageHandler(project), selectedScope)
}

private fun <O> findUsages(showDialog: Boolean, project: Project, symbol: Symbol, handler: UsageHandler<O>, selectedScope: SearchScope) {
  val allOptions = AllSearchOptions(
    options = UsageOptions.createOptions(true, selectedScope),
    textSearch = if (symbol.hasTextSearchStrings()) false else null,
    customOptions = handler.getCustomOptions(UsageHandler.UsageAction.FIND_USAGES)
  )
  findUsages(showDialog, project, symbol, handler, allOptions)
}

private fun <O> findUsages(showDialog: Boolean,
                           project: Project,
                           symbol: Symbol,
                           handler: UsageHandler<O>,
                           allOptions: AllSearchOptions<O>) {
  if (showDialog) {
    val canReuseTab = canReuseTab(project)
    val dialog = UsageOptionsDialog(project, symbol.presentableText(), handler, allOptions, canReuseTab)
    if (!dialog.showAndGet()) {
      // cancelled
      return
    }
    findUsages(project, symbol, handler, dialog.result())
  }
  else {
    findUsages(project, symbol, handler, allOptions)
  }
}

internal fun <O> findUsages(project: Project, symbol: Symbol, handler: UsageHandler<O>, allOptions: AllSearchOptions<O>) {
  val query = buildQuery(project, symbol, handler, allOptions)
  val factory = Factory {
    UsageSearcher {
      runSearch(project, query, it)
    }
  }
  val usageViewPresentation = UsageViewPresentation().apply {
    searchString = handler.getSearchString(allOptions)
    scopeText = allOptions.options.searchScope.displayName
    tabText = UsageViewBundle.message("search.title.0.in.1", searchString, scopeText)
    isOpenInNewTab = FindSettings.getInstance().isShowResultsInSeparateView || !canReuseTab(project)
  }
  project.service<UsageViewManager>().searchAndShowUsages(
    arrayOf(SymbolUsageTarget(project, symbol, allOptions)),
    factory,
    false,
    true,
    usageViewPresentation,
    null
  )
}

private fun canReuseTab(project: Project): Boolean {
  val contentManager = UsageViewContentManager.getInstance(project)
  val selectedContent = contentManager.getSelectedContent(true)
  return if (selectedContent == null) {
    contentManager.reusableContentsCount != 0
  }
  else {
    !selectedContent.isPinned
  }
}

internal fun <O> UsageHandler<O>.getSearchString(allOptions: AllSearchOptions<O>): String {
  return getSearchString(allOptions.options, allOptions.customOptions)
}

internal fun <O> buildQuery(project: Project, symbol: Symbol, handler: UsageHandler<O>, allOptions: AllSearchOptions<O>): Query<out Usage> {
  val (options, textSearch, customOptions) = allOptions
  val usageQuery = handler.buildSearchQuery(options, customOptions)
  if (textSearch != true) {
    return usageQuery
  }
  val textSearchStrings = symbol.getTextSearchStrings()
  if (textSearchStrings.isEmpty()) {
    return usageQuery
  }
  val queries = ArrayList<Query<out Usage>>(textSearchStrings.size + 1)
  queries += usageQuery
  textSearchStrings.mapTo(queries) {
    buildTextQuery(project, it, options.searchScope)
  }
  return SearchService.getInstance().merge(queries)
}
