// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker.metadata

import com.intellij.internal.ml.completion.RankingModelProvider
import com.jetbrains.completion.ranker.KotlinMLRankingProvider
import com.jetbrains.completion.ranker.PythonMLRankingProvider
import org.junit.Test

class MetadataConsistencyTest {
  @Test
  fun testKotlinMetadata() = doTest(KotlinMLRankingProvider())

  @Test
  fun testPythonMetadata() = doTest(PythonMLRankingProvider())

  private fun doTest(provider: RankingModelProvider) {
    val model = provider.model
    val order = model.featuresOrder
    model.predict(DoubleArray(order.size))
  }
}