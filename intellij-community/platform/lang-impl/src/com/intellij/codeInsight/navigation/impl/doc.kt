// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.impl

import com.intellij.codeInsight.navigation.CtrlMouseDocInfo
import com.intellij.codeInsight.navigation.SingleTargetElementInfo
import com.intellij.model.Symbol
import com.intellij.model.documentation.impl.getSymbolDocumentation
import com.intellij.model.psi.PsiSymbolService
import com.intellij.psi.PsiElement

internal fun docInfo(symbol: Symbol, elementAtPointer: PsiElement): CtrlMouseDocInfo {
  return fromPsi(symbol, elementAtPointer)
         ?: CtrlMouseDocInfo(getSymbolDocumentation(symbol), null, null)
}

private fun fromPsi(symbol: Symbol, elementAtPointer: PsiElement): CtrlMouseDocInfo? {
  val psi = PsiSymbolService.getInstance().extractElementFromSymbol(symbol) ?: return null
  val info = SingleTargetElementInfo.generateInfo(psi, elementAtPointer, true)
  return info.takeUnless {
    it === CtrlMouseDocInfo.EMPTY
  }
}
