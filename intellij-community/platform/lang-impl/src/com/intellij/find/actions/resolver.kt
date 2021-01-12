// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.find.actions

import com.intellij.CommonBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.navigation.PsiElementTargetPopupPresentation
import com.intellij.find.FindBundle
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.find.usages.SearchTarget
import com.intellij.find.usages.impl.DefaultSymbolSearchTarget
import com.intellij.find.usages.impl.symbolSearchTargets
import com.intellij.model.psi.PsiSymbolService
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
    searchTargets(dataContext),
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

private fun allTargets(project: Project, targets: Collection<SearchTarget>, oldTargets: Array<out UsageTarget>): List<TargetVariant> {
  val allTargets = ArrayList<TargetVariant>()
  targets.mapTo(allTargets, TargetVariant::SearchTargetVariant)
  for (usageTarget in oldTargets) {
    if (!usageTarget.isValid || containsElementFromUsageTarget(project, targets, usageTarget)) {
      // usage target is a simple PsiElement target
      // => symbols should contain it too
      // => if so, then we skip it to avoid duplicate items (and to avoid showing the popup, which is showed if there are > 1 items)
    }
    else {
      allTargets.add(TargetVariant.UsageTargetVariant(usageTarget))
    }
  }
  return allTargets
}

private fun containsElementFromUsageTarget(project: Project, targets: Collection<SearchTarget>, oldTarget: UsageTarget): Boolean {
  if (oldTarget !is PsiElement2UsageTargetAdapter) {
    return false
  }
  val targetElement = oldTarget.element ?: return false
  val manager = PsiManager.getInstance(project)
  fun isWrappedTargetElement(target: SearchTarget): Boolean {
    val element = targetPsi(target)
    return element != null && manager.areElementsEquivalent(element, targetElement)
  }
  return targets.any(::isWrappedTargetElement)
}

private fun searchTargets(dataContext: DataContext): List<SearchTarget> {
  val file = dataContext.getData(CommonDataKeys.PSI_FILE) ?: return emptyList()
  val offset: Int = dataContext.getData(CommonDataKeys.CARET)?.offset ?: return emptyList()
  return symbolSearchTargets(file, offset)
}

private sealed class TargetVariant {
  class SearchTargetVariant(val target: SearchTarget) : TargetVariant()
  class UsageTargetVariant(val target: UsageTarget) : TargetVariant()
}

internal interface UsageVariantHandler {
  fun handleTarget(target: SearchTarget)
  fun handlePsi(element: PsiElement)
}

private fun UsageVariantHandler.handle(targetVariant: TargetVariant) {
  when (targetVariant) {
    is TargetVariant.SearchTargetVariant -> {
      handlePsiOrSymbol(targetVariant.target)
    }
    is TargetVariant.UsageTargetVariant -> {
      val target = targetVariant.target
      if (target is PsiElement2UsageTargetAdapter) {
        handlePsi(target.element)
      }
      else {
        target.findUsages() // custom target
      }
    }
  }
}

internal fun UsageVariantHandler.handlePsiOrSymbol(target: SearchTarget) {
  val element = targetPsi(target)
  if (element != null) {
    handlePsi(element)
  }
  else {
    handleTarget(target)
  }
}

private fun getPresentation(targetVariant: TargetVariant): TargetPopupPresentation {
  return when (targetVariant) {
    is TargetVariant.SearchTargetVariant -> targetVariant.target.presentation
    is TargetVariant.UsageTargetVariant -> {
      val target = targetVariant.target
      if (target is PsiElement2UsageTargetAdapter) {
        PsiElementTargetPopupPresentation(target.element)
      }
      else {
        Item2TargetPresentation(target.presentation!!)
      }
    }
  }
}

internal fun targetPsi(target: SearchTarget): PsiElement? {
  if (target is DefaultSymbolSearchTarget) {
    return PsiSymbolService.getInstance().extractElementFromSymbol(target.symbol)
  }
  else {
    return null
  }
}
