// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.ranker.metadata

import com.jetbrains.completion.ranker.FallbackKotlinMLRankingProvider
import com.jetbrains.completion.ranker.PythonMLRankingProvider
import org.junit.Test

class MetadataConsistencyTest {
  @Test
  fun testKotlinMetadata() = FallbackKotlinMLRankingProvider().assertModelMetadataConsistent()

  @Test
  fun testPythonMetadata() = PythonMLRankingProvider().assertModelMetadataConsistent()
}