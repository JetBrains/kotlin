// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.impl

import com.intellij.codeInsight.navigation.CtrlMouseInfo
import com.intellij.model.Symbol
import com.intellij.model.psi.impl.TargetData
import com.intellij.model.psi.impl.declaredReferencedData
import com.intellij.psi.PsiFile

internal fun gotoDeclarationOrUsages(file: PsiFile, offset: Int): GTDUActionData? {
  return processInjectionThenHost(file, offset, ::gotoDeclarationOrUsagesInner)
}

/**
 * "Go To Declaration Or Usages" action data
 */
internal interface GTDUActionData {

  fun ctrlMouseInfo(): CtrlMouseInfo

  fun result(): GTDUActionResult?
}

/**
 * "Go To Declaration Or Usages" action result
 */
internal sealed class GTDUActionResult {
  /**
   * Go To Declaration
   */
  class GTD(val gtdActionResult: GTDActionResult) : GTDUActionResult()

  /**
   * Show Usages
   */
  class SU(val targets: List<Symbol>) : GTDUActionResult() {

    init {
      require(targets.isNotEmpty())
    }
  }
}

private fun gotoDeclarationOrUsagesInner(file: PsiFile, offset: Int): GTDUActionData? {
  return fromDirectNavigation(file, offset)?.toGTDUActionData()
         ?: fromTargetData(file, offset)
}

private fun fromTargetData(file: PsiFile, offset: Int): GTDUActionData? {
  val (declaredData, referencedData) = declaredReferencedData(file, offset)
  return referencedData?.toGTDActionData(file.project)?.toGTDUActionData() // GTD of referenced symbols
         ?: referencedData?.let(::ShowUsagesGTDUActionData) // SU of referenced symbols if nowhere to navigate
         ?: declaredData?.let(::ShowUsagesGTDUActionData) // SU of declared symbols
}

internal fun GTDActionData.toGTDUActionData(): GTDUActionData? {
  val gtdActionResult = result() ?: return null                           // nowhere to navigate
  return object : GTDUActionData {
    override fun ctrlMouseInfo(): CtrlMouseInfo = this@toGTDUActionData.ctrlMouseInfo()
    override fun result(): GTDUActionResult = GTDUActionResult.GTD(gtdActionResult)
  }
}

private class ShowUsagesGTDUActionData(private val targetData: TargetData) : GTDUActionData {
  override fun ctrlMouseInfo(): CtrlMouseInfo = targetData.ctrlMouseInfo()
  override fun result(): GTDUActionResult? = targetData.targets.let { targets ->
    if (targets.isEmpty()) {
      null
    }
    else {
      GTDUActionResult.SU(targets)
    }
  }
}
