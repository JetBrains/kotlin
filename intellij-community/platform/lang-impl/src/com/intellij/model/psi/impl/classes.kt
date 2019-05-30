// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

internal data class DeclaredReferencedData(
  val declaredData: TargetData?,
  val referencedData: TargetData?
)

internal sealed class TargetData {

  abstract val targets: List<Symbol>

  class Declared(val declaration: PsiSymbolDeclaration) : TargetData() {
    override val targets: List<Symbol> get() = listOf(declaration.symbol)
  }

  class Referenced(val references: List<PsiSymbolReference>) : TargetData() {

    init {
      require(references.isNotEmpty())
    }

    override val targets: List<Symbol>
      get() = references.flatMap { reference ->
        reference.resolveReference()
      }.map { resolveResult ->
        resolveResult.target
      }
  }

  class Evaluator(val origin: PsiOrigin, val targetElements: List<PsiElement>) : TargetData() {

    init {
      require(targetElements.isNotEmpty())
    }

    override val targets: List<Symbol> get() = targetElements.map(PsiSymbolService.getInstance()::asSymbol)
  }
}

internal sealed class PsiOrigin {
  class Leaf(val leaf: PsiElement) : PsiOrigin()
  class Reference(val reference: PsiReference) : PsiOrigin()
}
