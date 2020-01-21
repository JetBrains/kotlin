// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.psi.PsiFile
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus.Experimental

/**
 * Entry point for obtaining target symbols by [offset] in a [file].
 *
 * @return collection of referenced or declared symbols
 */
@Experimental
fun targetSymbols(file: PsiFile, offset: Int): Collection<Symbol> {
  val referencedSymbols: Collection<Symbol> = referencedSymbols(file, offset)
  if (referencedSymbols.isNotEmpty()) {
    return referencedSymbols
  }
  return declaredSymbols(file, offset)
}

private fun referencedSymbols(file: PsiFile, offset: Int): Collection<Symbol> {
  val result = SmartList<Symbol>()
  for (reference: PsiSymbolReference in file.allReferencesAround(offset)) {
    reference.resolveReference().mapTo(result) {
      it.target
    }
  }
  return result
}

private fun declaredSymbols(file: PsiFile, offset: Int): Collection<Symbol> {
  val result = SmartList<Symbol>()
  file.allDeclarationsAround(offset).mapTo(result, PsiSymbolDeclaration::getSymbol)
  return result
}
