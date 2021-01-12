// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages.impl

import com.intellij.find.usages.SearchTarget
import com.intellij.find.usages.SymbolSearchTargetFactory
import com.intellij.find.usages.SymbolUsageHandlerFactory
import com.intellij.find.usages.UsageHandler
import com.intellij.model.Symbol
import com.intellij.model.psi.impl.targetSymbols
import com.intellij.model.search.SearchService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClassExtension
import com.intellij.psi.PsiFile
import com.intellij.usages.Usage
import com.intellij.util.Query
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
fun symbolSearchTargets(file: PsiFile, offset: Int): List<SearchTarget> {
  val targetSymbols = targetSymbols(file, offset)
  if (targetSymbols.isEmpty()) {
    return emptyList()
  }
  return symbolSearchTargets(file.project, targetSymbols)
}

internal fun symbolSearchTargets(project: Project, targetSymbols: Collection<Symbol>): List<SearchTarget> {
  return targetSymbols.mapTo(LinkedHashSet()) {
    symbolSearchTarget(project, it)
  }.toList()
}

private val SEARCH_TARGET_EXTENSION = ClassExtension<SymbolSearchTargetFactory<*>>("com.intellij.lang.symbolSearchTarget")

fun symbolSearchTarget(project: Project, symbol: Symbol): SearchTarget {
  for (factory in SEARCH_TARGET_EXTENSION.forKey(symbol.javaClass)) {
    @Suppress("UNCHECKED_CAST")
    val factory_ = factory as SymbolSearchTargetFactory<Symbol>
    val target = factory_.createTarget(project, symbol)
    if (target != null) {
      return target
    }
  }
  return DefaultSymbolSearchTarget(project, symbol)
}

private val USAGE_HANDLER_EXTENSION = ClassExtension<SymbolUsageHandlerFactory<*>>("com.intellij.lang.symbolUsageHandler")

internal fun symbolUsageHandler(project: Project, symbol: Symbol): UsageHandler<*> {
  for (factory in USAGE_HANDLER_EXTENSION.forKey(symbol.javaClass)) {
    @Suppress("UNCHECKED_CAST")
    val factory_ = factory as SymbolUsageHandlerFactory<Symbol>
    val handler = factory_.createHandler(project, symbol)
    if (handler != null) {
      return handler
    }
  }
  return DefaultSymbolUsageHandler(project, symbol)
}

@ApiStatus.Internal
fun <O> buildQuery(project: Project,
                   target: SearchTarget,
                   handler: UsageHandler<O>,
                   allOptions: AllSearchOptions<O>): Query<out Usage> {
  val (options, textSearch, customOptions) = allOptions
  val usageQuery = handler.buildSearchQuery(options, customOptions)
  if (textSearch != true) {
    return usageQuery
  }
  val textSearchStrings = target.textSearchStrings
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
