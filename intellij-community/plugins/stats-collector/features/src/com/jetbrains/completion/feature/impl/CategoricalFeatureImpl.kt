/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.completion.feature.impl

import com.jetbrains.completion.feature.CategoricalFeature

class CategoricalFeatureImpl(override val name: String,
                             private val categoryToIndex: Map<String, Int>)
  : CategoricalFeature {
  override val undefinedIndex: Int? = categoryToIndex[FeatureUtils.UNDEFINED]
  override val otherCategoryIndex: Int? = categoryToIndex[FeatureUtils.OTHER]

  override fun indexByCategory(category: String): Int? = categoryToIndex[category]

  override val categories: Set<String> = categoryToIndex.keys

  override fun process(value: Any, featureArray: DoubleArray) {
    setDefaults(featureArray)
    setUndefined(0.0, featureArray)
    val index = indexByCategory(value.toString())
    if (index == null) {
      setOther(1.0, featureArray)
    }
    else {
      featureArray[index] = 1.0
    }
  }

  override fun setDefaults(featureArray: DoubleArray) {
    categoryToIndex.values.forEach { featureArray[it] = 0.0 }
    setUndefined(1.0, featureArray)
    setOther(0.0, featureArray)
  }

  private fun setUndefined(value: Double, featureArray: DoubleArray) {
    if (undefinedIndex != null) {
      featureArray[undefinedIndex] = value
    }
  }

  private fun setOther(value: Double, featureArray: DoubleArray) {
    if (otherCategoryIndex != null) {
      featureArray[otherCategoryIndex] = value
    }
  }
}