// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.feature

interface ModelMetadataEx : ModelMetadata {
  val binary: List<BinaryFeature>
  val float: List<DoubleFeature>
  val categorical: List<CategoricalFeature>

  val featuresOrder: Map<String, Int>
  val version: String?
}