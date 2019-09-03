// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

interface RankingModelWrapper {
  fun version(): String?

  fun shouldSort(features: RankingFeatures): Boolean

  fun score(features: RankingFeatures): Double?

  companion object {
    val DISABLED = object : RankingModelWrapper {
      override fun shouldSort(features: RankingFeatures): Boolean = false

      override fun score(features: RankingFeatures): Double? = null

      override fun version(): String? = null
    }
  }
}
