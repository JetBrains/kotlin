// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.presentation.impl

import com.intellij.model.presentation.SymbolPresentation
import javax.swing.Icon

internal class DefaultSymbolPresentation(
  private val icon: Icon?,
  private val typeString: String,
  private val shortNameString: String,
  private val longNameString: String? = shortNameString
) : SymbolPresentation {
  override fun getIcon(): Icon? = icon
  override fun getShortNameString(): String = shortNameString
  override fun getShortDescription(): String = "$typeString '$shortNameString'"
  override fun getLongDescription(): String = "$typeString '$longNameString'"
}
