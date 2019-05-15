// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker.metadata

import com.jetbrains.completion.feature.ModelMetadataEx
import com.jetbrains.completion.feature.ModelMetadataTest
import com.jetbrains.completion.ranker.JavaCompletionRanker
import com.jetbrains.completion.ranker.KotlinCompletionRanker
import com.jetbrains.completion.ranker.PythonCompletionRanker

class PythonMetadataTest : ModelMetadataTest() {
  override fun modelMetadata(): ModelMetadataEx = PythonCompletionRanker().modelMetadata
}

class KotlinMetadataTest : ModelMetadataTest() {
  override fun modelMetadata(): ModelMetadataEx = KotlinCompletionRanker().modelMetadata
}

class JavaMetadataTest : ModelMetadataTest() {
  override fun modelMetadata(): ModelMetadataEx = JavaCompletionRanker().modelMetadata
}