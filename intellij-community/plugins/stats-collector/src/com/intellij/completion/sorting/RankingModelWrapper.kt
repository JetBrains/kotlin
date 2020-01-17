// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

interface RankingModelWrapper {
  fun version(): String?

  fun canScore(features: RankingFeatures): Boolean

  fun score(features: RankingFeatures): Double?
}
