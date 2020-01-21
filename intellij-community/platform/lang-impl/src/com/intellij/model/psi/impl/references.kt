// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceHints
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementsAroundOffsetUp
import org.jetbrains.annotations.ApiStatus.Experimental

/**
 * @return iterable of [references][PsiSymbolReferenceService.getReferences] around given [offset]
 */
@Experimental
fun PsiFile.referencesAround(offset: Int): Iterable<PsiSymbolReference> {
  val service: PsiSymbolReferenceService = PsiSymbolReferenceService.getService()
  for ((element, offsetInElement) in elementsAroundOffsetUp(offset)) {
    val hints = PsiSymbolReferenceHints.offsetHint(offsetInElement)
    val references: Collection<PsiSymbolReference> = service.getReferences(element, hints)
    if (references.isNotEmpty()) {
      return references
    }
  }
  return emptyList()
}
