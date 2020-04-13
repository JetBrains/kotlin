// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages.impl

import com.intellij.find.usages.SymbolUsageHandlerFactory
import com.intellij.find.usages.UsageHandler
import com.intellij.model.Symbol
import com.intellij.model.search.SearchService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClassExtension
import com.intellij.usages.Usage
import com.intellij.util.Query

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
