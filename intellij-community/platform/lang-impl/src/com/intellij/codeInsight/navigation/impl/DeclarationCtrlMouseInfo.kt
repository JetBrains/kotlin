// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.impl

import com.intellij.codeInsight.navigation.BaseCtrlMouseInfo
import com.intellij.codeInsight.navigation.CtrlMouseDocInfo
import com.intellij.model.psi.PsiSymbolDeclaration

internal class DeclarationCtrlMouseInfo(
  private val declaration: PsiSymbolDeclaration
) : BaseCtrlMouseInfo(listOf(declaration.absoluteRange)) {

  override fun isValid(): Boolean = declaration.declaringElement.isValid

  override fun getDocInfo(): CtrlMouseDocInfo = docInfo(declaration.symbol, declaration.declaringElement)
}
