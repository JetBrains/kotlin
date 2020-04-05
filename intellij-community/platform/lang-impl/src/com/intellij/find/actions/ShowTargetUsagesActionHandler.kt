// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions

import com.intellij.find.FindSettings
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.usages.UsageHandler
import com.intellij.find.usages.UsageOptions.createOptions
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService.getLongDescription
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IntRef
import com.intellij.psi.search.SearchScope
import com.intellij.ui.awt.RelativePoint
import com.intellij.usages.UsageSearchPresentation
import com.intellij.usages.UsageSearcher

// data class for `copy` method
internal data class ShowTargetUsagesActionHandler<O>(
  private val project: Project,
  private val editor: Editor?,
  private val popupPosition: RelativePoint,
  private val minWidth: IntRef,
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

  override fun showDialogAndShowUsages(newEditor: Editor?) {
    val dialog = UsageOptionsDialog(project, getLongDescription(symbol), usageHandler, allOptions, false)
    if (!dialog.showAndGet()) {
      // cancelled
      return
    }
    copy(editor = newEditor, allOptions = dialog.result()).showUsages()
  }

  override fun showUsagesInScope(searchScope: SearchScope) {
    copy(allOptions = allOptions.copy(options = createOptions(allOptions.options.isUsages, searchScope))).showUsages()
  }

  override fun findUsages(): Unit = findUsages(project, symbol, usageHandler, allOptions)

  override fun getSelectedScope(): SearchScope = allOptions.options.searchScope

  override fun getMaximalScope(): SearchScope = usageHandler.maximalSearchScope

  fun showUsages() {
    val query = buildQuery(project, symbol, usageHandler, allOptions)
    val usageSearcher = UsageSearcher {
      query.forEach(it)
    }
    ShowUsagesAction.showElementUsages(
      project,
      editor,
      popupPosition,
      ShowUsagesAction.getUsagesPageSize(),
      IntRef(0),
      usageSearcher,
      this
    )
  }

  companion object {

    @JvmStatic
    fun showUsages(project: Project, dataContext: DataContext, symbol: Symbol) {
      create(project, dataContext, symbol, symbol.createUsageHandler(project)).showUsages()
    }

    private fun <O> create(project: Project,
                           dataContext: DataContext,
                           symbol: Symbol,
                           usageHandler: UsageHandler<O>): ShowTargetUsagesActionHandler<O> {
      val editor = dataContext.getData(CommonDataKeys.EDITOR)
      val popupPosition = JBPopupFactory.getInstance().guessBestPopupLocation(dataContext)
      val searchScope = FindUsagesOptions.findScopeByName(project, dataContext, FindSettings.getInstance().defaultScopeName)
      return ShowTargetUsagesActionHandler(
        project, editor, popupPosition, IntRef(0),
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
