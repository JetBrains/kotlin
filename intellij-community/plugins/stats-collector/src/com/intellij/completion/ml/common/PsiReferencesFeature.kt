// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.ElementFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.impl.search.PsiSearchHelperImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import java.util.concurrent.atomic.AtomicInteger

class PsiReferencesFeature : ElementFeatureProvider {

  override fun getName(): String = "references"

  override fun calculateFeatures(element: LookupElement,
                                 location: CompletionLocation,
                                 contextFeatures: ContextFeatures): MutableMap<String, MLFeatureValue> {
    val psiElement = element.psiElement ?: return mutableMapOf()

    val searchHelper = PsiSearchHelper.getInstance(location.project) as PsiSearchHelperImpl
    val useScope = psiElement.useScope as? GlobalSearchScope ?: return mutableMapOf()

    val referencingFiles = AtomicInteger(0)
    searchHelper.processFilesWithText(useScope, UsageSearchContext.IN_CODE, true, element.lookupString) {
      referencingFiles.incrementAndGet()
      true
    }

    return mutableMapOf("file_count" to MLFeatureValue.float(referencingFiles.get()))
  }
}