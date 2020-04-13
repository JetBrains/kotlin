// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.find.actions

import com.intellij.find.FindSettings
import com.intellij.find.usages.UsageHandler
import com.intellij.find.usages.UsageOptions
import com.intellij.find.usages.impl.AllSearchOptions
import com.intellij.find.usages.impl.buildQuery
import com.intellij.find.usages.impl.createUsageHandler
import com.intellij.find.usages.impl.hasTextSearchStrings
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService.getLongDescription
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Factory
import com.intellij.psi.impl.search.runSearch
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewContentManager
import com.intellij.usages.UsageSearcher
import com.intellij.usages.UsageViewManager
import com.intellij.usages.UsageViewPresentation
import org.jetbrains.annotations.ApiStatus

internal val LOG: Logger = Logger.getInstance("#com.intellij.find.actions")

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
    val dialog = UsageOptionsDialog(project, getLongDescription(symbol), handler, allOptions, canReuseTab)
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
