// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.ElementFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.impl.cache.impl.id.IdIndex
import com.intellij.psi.impl.cache.impl.id.IdIndexEntry
import com.intellij.psi.search.UsageSearchContext
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl
import java.util.concurrent.atomic.AtomicInteger

class TextReferencesFeature : ElementFeatureProvider {

  override fun getName(): String = "references"

  override fun calculateFeatures(element: LookupElement,
                                 location: CompletionLocation,
                                 contextFeatures: ContextFeatures): MutableMap<String, MLFeatureValue> {
    if (element.psiElement == null || !StringUtil.isJavaIdentifier(element.lookupString)) return mutableMapOf()

    val project = location.project
    val index = FileBasedIndex.getInstance() as FileBasedIndexImpl
    val filter = index.projectIndexableFiles(project) ?: return mutableMapOf()
    val referencingFiles = AtomicInteger(0)
    index.processAllValues(IdIndex.NAME, IdIndexEntry(element.lookupString, true), project) { fileId, value ->
      if (value and UsageSearchContext.IN_CODE.toInt() != 0 && filter.containsFileId (fileId)) {
        referencingFiles.incrementAndGet()
      }
      true
    }

    return mutableMapOf("file_count" to MLFeatureValue.float(referencingFiles.get()))
  }
}