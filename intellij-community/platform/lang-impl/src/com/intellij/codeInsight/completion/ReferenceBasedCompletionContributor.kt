// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion

import com.intellij.model.psi.PsiCompletableReference
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.impl.referencesAt
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement

class ReferenceBasedCompletionContributor : CompletionContributor() {

  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    if (parameters.completionType != CompletionType.BASIC) {
      return
    }
    val fileOffset: Int = parameters.offset
    for (reference: PsiSymbolReference in parameters.position.containingFile.referencesAt(fileOffset)) {
      ProgressManager.checkCanceled()

      if (reference !is PsiCompletableReference) {
        continue
      }

      val variants: Collection<Any?> = reference.completionVariants
      if (variants.isEmpty()) {
        continue
      }

      ProgressManager.checkCanceled()

      val element: PsiElement = reference.getElement()
      val beginIndex: Int = reference.rangeInElement.startOffset
      val offsetInElement: Int = fileOffset - element.textRange.startOffset
      val prefix: String = element.text.substring(beginIndex, offsetInElement)
      val resultWithPrefix: CompletionResultSet = result.withPrefixMatcher(prefix)

      for (variant: Any? in variants) {
        if (variant != null) {
          @Suppress("DEPRECATION")
          resultWithPrefix.addElement(CompletionData.objectToLookupItem(variant))
        }
      }
    }
  }
}
