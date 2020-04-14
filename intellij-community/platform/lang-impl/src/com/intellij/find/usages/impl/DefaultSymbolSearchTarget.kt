// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.usages.impl

import com.intellij.find.usages.SearchTarget
import com.intellij.find.usages.UsageHandler
import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService
import com.intellij.navigation.TargetPopupPresentation
import com.intellij.openapi.project.Project
import com.intellij.util.lazyPub

internal class DefaultSymbolSearchTarget(
  private val project: Project,
  val symbol: Symbol
) : SearchTarget {

  override fun createPointer(): Pointer<out SearchTarget> = MyPointer(project, symbol)

  override val presentation: TargetPopupPresentation by lazyPub {
    SymbolPresentationService.getInstance().getPopupPresentation(symbol)
  }

  override val usageHandler: UsageHandler<*> by lazyPub {
    symbolUsageHandler(project, symbol)
  }

  override val textSearchStrings: Collection<String> by lazyPub {
    symbol.getTextSearchStrings()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DefaultSymbolSearchTarget

    if (symbol != other.symbol) return false

    return true
  }

  override fun hashCode(): Int {
    return symbol.hashCode()
  }

  private class MyPointer(private val project: Project, symbol: Symbol) : Pointer<DefaultSymbolSearchTarget> {

    private val mySymbolPointer: Pointer<out Symbol> = symbol.createPointer()

    override fun dereference(): DefaultSymbolSearchTarget? {
      val symbol = mySymbolPointer.dereference() ?: return null
      return DefaultSymbolSearchTarget(project, symbol)
    }
  }
}
