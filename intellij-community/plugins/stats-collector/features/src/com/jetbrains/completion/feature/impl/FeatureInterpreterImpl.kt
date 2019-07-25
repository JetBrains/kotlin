/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import com.jetbrains.completion.feature.BinaryFeature
import com.jetbrains.completion.feature.BinaryFeature.BinaryValueDescriptor
import com.jetbrains.completion.feature.CategoricalFeature
import com.jetbrains.completion.feature.DoubleFeature
import com.jetbrains.completion.feature.FeatureInterpreter
import com.jetbrains.completion.feature.ex.FeatureDefaultValueNotFound
import com.jetbrains.completion.feature.ex.InconsistentMetadataException

class FeatureInterpreterImpl : FeatureInterpreter {
  override fun binary(name: String, description: Map<String, Any>, order: Map<String, Int>): BinaryFeature {
    val index = extractIndex(name, order)
    val undefinedIndex = extractUndefinedIndex(name, order)
    val default = description[FeatureUtils.DEFAULT].toString().toDoubleOrNull() ?: throw FeatureDefaultValueNotFound(name)
    val values = extractBinaryValuesMapping(description)
    return BinaryFeatureImpl(name, index, undefinedIndex, default, values.first, values.second)
  }

  override fun double(name: String, description: Map<String, Any>, order: Map<String, Int>): DoubleFeature {
    val index = extractIndex(name, order)
    val undefinedIndex = extractUndefinedIndex(name, order)
    val defaultValue = description[FeatureUtils.DEFAULT].toString().toDoubleOrNull() ?: throw FeatureDefaultValueNotFound(name)
    return DoubleFeatureImpl(name, index, undefinedIndex, defaultValue)
  }

  override fun categorical(name: String, categories: List<String>, order: Map<String, Int>): CategoricalFeature {
    val categoryToIndex = categories.associate { it to extractIndex(combine(name, it), order) }
    return CategoricalFeatureImpl(name, categoryToIndex)
  }

  private fun extractIndex(name: String, order: Map<String, Int>): Int {
    return order[name] ?: throw InconsistentMetadataException("Feature with name '$name' not found in feature_order.txt")
  }

  private fun extractUndefinedIndex(name: String, order: Map<String, Int>): Int? {
    return order[FeatureUtils.getUndefinedFeatureName(name)]
  }

  private fun extractBinaryValuesMapping(description: Map<String, Any>)
    : Pair<BinaryValueDescriptor, BinaryValueDescriptor> {
    val result = mutableListOf<BinaryValueDescriptor>()
    for ((name, value) in description) {
      if (name == FeatureUtils.DEFAULT || name == FeatureUtils.USE_UNDEFINED) continue
      val mappedValue = value.toString().toDoubleOrNull()
      if (mappedValue == null) throw InconsistentMetadataException("mapped value for binary feature should be double")
      result += BinaryValueDescriptor(name, mappedValue)
    }

    assert(result.size == 2) { "binary feature must contains 2 values, but found $result" }
    result.sortBy { it.key }
    return result[0] to result[1]
  }

  private fun combine(featureName: String, categoryName: String): String = "$featureName=$categoryName"
}