// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.impl

import com.intellij.codeInsight.navigation.BaseCtrlMouseInfo
import com.intellij.codeInsight.navigation.CtrlMouseDocInfo
import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.model.documentation.impl.getSymbolDocumentation
import com.intellij.openapi.util.TextRange

internal class SingleSymbolCtrlMouseInfo(symbol: Symbol, textRanges: List<TextRange>) : BaseCtrlMouseInfo(textRanges) {

  private val pointer: Pointer<out Symbol> = symbol.createPointer()

  private val symbol: Symbol
    get() = requireNotNull(pointer.dereference()) {
      "Must not be called on invalid info"
    }

  override fun isValid(): Boolean = pointer.dereference() != null

  override fun isNavigatable(): Boolean = true

  override fun getDocInfo(): CtrlMouseDocInfo = CtrlMouseDocInfo(getSymbolDocumentation(symbol), null, null)
}
