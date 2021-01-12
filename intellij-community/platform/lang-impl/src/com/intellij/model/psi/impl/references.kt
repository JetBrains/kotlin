// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.model.SymbolResolveResult
import com.intellij.model.psi.ImplicitReferenceProvider
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceHints
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementsAroundOffsetUp
import com.intellij.psi.util.elementsAtOffsetUp

/**
 * @return collection of [references][PsiSymbolReferenceService.getReferences] to the right of given [offset]
 */
fun PsiFile.referencesAt(offset: Int): Collection<PsiSymbolReference> {
  for ((element, offsetInElement) in elementsAtOffsetUp(offset)) {
    val references = referencesInElement(element, offsetInElement)
    if (references.isNotEmpty()) {
      return references
    }
  }
  return emptyList()
}

/**
 * @return collection of [references][PsiSymbolReferenceService.getReferences]
 * and [implicit references][ImplicitReferenceProvider] around given [offset]
 */
fun PsiFile.allReferencesAround(offset: Int): Collection<PsiSymbolReference> {
  for ((element, offsetInElement) in elementsAroundOffsetUp(offset)) {
    val referencesInElement = allReferencesInElement(element, offsetInElement)
    if (referencesInElement.isNotEmpty()) {
      return referencesInElement
    }
  }
  return emptyList()
}

fun allReferencesInElement(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolReference> {
  val references: Collection<PsiSymbolReference> = referencesInElement(element, offsetInElement)
  if (references.isNotEmpty()) {
    return references
  }
  val implicitReference = implicitReference(element)
  if (implicitReference != null) {
    return listOf(implicitReference)
  }
  return emptyList()
}

private fun referencesInElement(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolReference> {
  val hints = PsiSymbolReferenceHints.offsetHint(offsetInElement)
  return PsiSymbolReferenceService.getService().getReferences(element, hints)
}

private fun implicitReference(element: PsiElement): PsiSymbolReference? {
  for (handler in ImplicitReferenceProvider.EP_NAME.extensions) {
    val resolved = handler.resolveAsReference(element)
    if (resolved.isNotEmpty()) {
      return ImmediatePsiSymbolReference(element, resolved)
    }
  }
  return null
}

private class ImmediatePsiSymbolReference(
  private val myElement: PsiElement,
  private val myResults: Collection<SymbolResolveResult>
) : PsiSymbolReference {

  private val myRange = TextRange.from(0, element.textLength)

  override fun getElement(): PsiElement = myElement
  override fun getRangeInElement(): TextRange = myRange
  override fun resolveReference(): Collection<SymbolResolveResult> = myResults
}
