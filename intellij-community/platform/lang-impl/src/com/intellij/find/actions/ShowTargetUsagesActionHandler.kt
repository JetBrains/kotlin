// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions

import com.intellij.find.FindSettings
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.usages.UsageHandler
import com.intellij.find.usages.UsageOptions.createOptions
import com.intellij.find.usages.impl.AllSearchOptions
import com.intellij.find.usages.impl.buildQuery
import com.intellij.find.usages.impl.createUsageHandler
import com.intellij.find.usages.impl.hasTextSearchStrings
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService.getLongDescription
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.search.SearchScope
import com.intellij.usages.UsageSearchPresentation
import com.intellij.usages.UsageSearcher

// data class for `copy` method
internal data class ShowTargetUsagesActionHandler<O>(
  private val project: Project,
  private val symbol: Symbol,
  private val usageHandler: UsageHandler<O>,
  private val allOptions: AllSearchOptions<O>
) : ShowUsagesActionHandler {

  override fun isValid(): Boolean = true

  override fun getPresentation(): UsageSearchPresentation {
    return UsageSearchPresentation {
      usageHandler.getSearchString(allOptions)
    }
  }

  override fun createUsageSearcher(): UsageSearcher {
    val query = buildQuery(project, symbol, usageHandler, allOptions)
    return UsageSearcher {
      query.forEach(it)
    }
  }

  override fun showDialog(): ShowUsagesActionHandler? {
    val dialog = UsageOptionsDialog(project, getLongDescription(symbol), usageHandler, allOptions, false)
    if (!dialog.showAndGet()) {
      // cancelled
      return null
    }
    else {
      return copy(allOptions = dialog.result())
    }
  }

  override fun withScope(searchScope: SearchScope): ShowUsagesActionHandler {
    return copy(allOptions = allOptions.copy(options = createOptions(allOptions.options.isUsages, searchScope)))
  }

  override fun findUsages(): Unit = findUsages(project, symbol, usageHandler, allOptions)

  override fun getSelectedScope(): SearchScope = allOptions.options.searchScope

  override fun getMaximalScope(): SearchScope = usageHandler.maximalSearchScope

  companion object {

    @JvmStatic
    fun showUsages(project: Project, dataContext: DataContext, symbol: Symbol) {
      val searchScope = FindUsagesOptions.findScopeByName(project, dataContext, FindSettings.getInstance().defaultScopeName)
      val showUsagesActionHandler = createActionHandler(project, searchScope, symbol, symbol.createUsageHandler(project))
      ShowUsagesAction.showElementUsages(ShowUsagesParameters.initial(project, dataContext), showUsagesActionHandler)
    }

    private fun <O> createActionHandler(project: Project,
                                        searchScope: SearchScope,
                                        symbol: Symbol,
                                        usageHandler: UsageHandler<O>): ShowTargetUsagesActionHandler<O> {
      return ShowTargetUsagesActionHandler(
        project,
        symbol = symbol,
        usageHandler = usageHandler,
        allOptions = AllSearchOptions(
          options = createOptions(searchScope),
          textSearch = if (symbol.hasTextSearchStrings()) false else null,
          customOptions = usageHandler.getCustomOptions(UsageHandler.UsageAction.SHOW_USAGES)
        )
      )
    }
  }
}
