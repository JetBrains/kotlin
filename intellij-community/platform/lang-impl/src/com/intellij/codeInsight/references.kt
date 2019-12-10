// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceHints
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementsAroundOffsetUp
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
fun PsiFile.referencesAt(offset: Int): Iterator<PsiSymbolReference> = sequence {
  val service: PsiSymbolReferenceService = PsiSymbolReferenceService.getService()
  for ((element: PsiElement, offsetInElement: Int) in elementsAroundOffsetUp(offset)) {
    val hints = PsiSymbolReferenceHints.offsetHint(offsetInElement)
    val references: Iterable<PsiSymbolReference> = service.getReferences(element, hints)
    yieldAll(references)
  }
}.iterator()
