// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.find.actions

import com.intellij.CommonBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.navigation.PsiElementTargetPopupPresentation
import com.intellij.find.FindBundle
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService
import com.intellij.model.psi.PsiSymbolService
import com.intellij.model.psi.impl.targetSymbols
import com.intellij.navigation.TargetPopupPresentation
import com.intellij.navigation.chooseTargetPopup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageView
import org.jetbrains.annotations.ApiStatus

/* This file contains weird logic so Symbols will work with PsiElements and UsageTargets. */

internal fun findShowUsages(project: Project, dataContext: DataContext, popupTitle: String, handler: UsageVariantHandler) {
  val allTargets = allTargets(
    project,
    targetSymbols(dataContext),
    dataContext.getData(UsageView.USAGE_TARGETS_KEY)?.get(0)?.let { arrayOf(it) } ?: UsageTarget.EMPTY_ARRAY
  )
  when (allTargets.size) {
    0 -> {
      val editor = dataContext.getData(CommonDataKeys.EDITOR)
      val message = FindBundle.message("find.no.usages.at.cursor.error")
      if (editor == null) {
        Messages.showMessageDialog(project, message, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
      }
      else {
        HintManager.getInstance().showErrorHint(editor, message)
      }
    }
    1 -> {
      handler.handle(allTargets.single())
    }
    else -> {
      chooseTargetPopup(
        popupTitle,
        allTargets,
        ::getPresentation,
        handler::handle
      ).showInBestPositionFor(dataContext)
    }
  }
}

private fun allTargets(project: Project, symbols: Collection<Symbol>, usageTargets: Array<out UsageTarget>): List<SymbolOrTarget> {
  val allTargets = ArrayList<SymbolOrTarget>()
  symbols.mapTo(allTargets) { SymbolOrTarget.S(it) }
  for (usageTarget in usageTargets) {
    if (!usageTarget.isValid || containsElementFromUsageTarget(project, symbols, usageTarget)) {
      // usage target is a simple PsiElement target
      // => symbols should contain it too
      // => if so, then we skip it to avoid duplicate items (and to avoid showing the popup, which is showed if there are > 1 items)
    }
    else {
      allTargets.add(SymbolOrTarget.UT(usageTarget))
    }
  }
  return allTargets
}

private fun containsElementFromUsageTarget(project: Project, symbols: Collection<Symbol>, usageTarget: UsageTarget): Boolean {
  if (usageTarget !is PsiElement2UsageTargetAdapter) {
    return false
  }
  val targetElement = usageTarget.element ?: return false
  val manager = PsiManager.getInstance(project)
  fun isWrappedTargetElement(symbol: Symbol): Boolean {
    val element = PsiSymbolService.getInstance().extractElementFromSymbol(symbol)
    return element != null && manager.areElementsEquivalent(element, targetElement)
  }
  return symbols.any(::isWrappedTargetElement)
}

private fun targetSymbols(dataContext: DataContext): List<Symbol> {
  val file = dataContext.getData(CommonDataKeys.PSI_FILE) ?: return emptyList()
  val offset: Int = dataContext.getData(CommonDataKeys.CARET)?.offset ?: return emptyList()
  return targetSymbols(file, offset).toList()
}

private sealed class SymbolOrTarget {
  class S(val symbol: Symbol) : SymbolOrTarget()
  class UT(val target: UsageTarget) : SymbolOrTarget()
}

internal interface UsageVariantHandler {
  fun handleSymbol(symbol: Symbol)
  fun handlePsi(element: PsiElement)
}

private fun UsageVariantHandler.handle(symbolOrTarget: SymbolOrTarget) {
  when (symbolOrTarget) {
    is SymbolOrTarget.S -> {
      val symbol = symbolOrTarget.symbol
      val element = PsiSymbolService.getInstance().extractElementFromSymbol(symbol)
      if (element != null) {
        handlePsi(element)
      }
      else {
        // this is new (non-adapted) Symbol implementation
        handleSymbol(symbol)
      }
    }
    is SymbolOrTarget.UT -> {
      val target = symbolOrTarget.target
      if (target is PsiElement2UsageTargetAdapter) {
        handlePsi(target.element)
      }
      else {
        target.findUsages() // custom target
      }
    }
  }
}

private fun getPresentation(symbolOrTarget: SymbolOrTarget): TargetPopupPresentation {
  return when (symbolOrTarget) {
    is SymbolOrTarget.S -> SymbolPresentationService.getInstance().getPopupPresentation(symbolOrTarget.symbol)
    is SymbolOrTarget.UT -> {
      val target = symbolOrTarget.target
      if (target is PsiElement2UsageTargetAdapter) {
        PsiElementTargetPopupPresentation(target.element)
      }
      else {
        Item2TargetPresentation(target.presentation!!)
      }
    }
  }
}
