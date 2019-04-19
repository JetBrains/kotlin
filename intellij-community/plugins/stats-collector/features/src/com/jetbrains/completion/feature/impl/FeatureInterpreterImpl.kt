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
import com.jetbrains.completion.feature.ex.FutureOrderNotFound

class FeatureInterpreterImpl : FeatureInterpreter {
    override fun binary(name: String, description: Map<String, Double>, order: Map<String, Int>): BinaryFeature {
        val index = extractIndex(name, order)
        val undefinedIndex = extractUndefinedIndex(name, order)
        val default = description[FeatureUtils.DEFAULT] ?: throw FeatureDefaultValueNotFound(name)
        val values = extractBinaryValuesMapping(description)
        return BinaryFeatureImpl(name, index, undefinedIndex, default, values.first, values.second)
    }

    override fun double(name: String, defaultValue: Double, order: Map<String, Int>): DoubleFeature {
        val index = extractIndex(name, order)
        val undefinedIndex = extractUndefinedIndex(name, order)
        return DoubleFeatureImpl(name, index, undefinedIndex, defaultValue)
    }

    override fun categorical(name: String, categories: Set<String>, order: Map<String, Int>): CategoricalFeature {
        val undefinedIndex = extractUndefinedIndex(name, order)
        val otherCategoryIndex = extractIndex(FeatureUtils.getOtherCategoryFeatureName(name), order)
        val categoryToIndex = categories.associate { it to extractIndex(combine(name, it), order) }
        return CategoricalFeatureImpl(name, undefinedIndex, otherCategoryIndex, categoryToIndex)
    }

    private fun extractIndex(name: String, order: Map<String, Int>): Int {
        return order[name] ?: throw FutureOrderNotFound(name)
    }

    private fun extractUndefinedIndex(name: String, order: Map<String, Int>): Int {
        return extractIndex(FeatureUtils.getUndefinedFeatureName(name), order)
    }

    private fun extractBinaryValuesMapping(description: Map<String, Double>)
            : Pair<BinaryValueDescriptor, BinaryValueDescriptor> {
        val result = mutableListOf<BinaryValueDescriptor>()
        for ((name, value) in description) {
            if (name == FeatureUtils.DEFAULT) continue
            result += BinaryValueDescriptor(name, value)
        }

        assert(result.size == 2, { "binary feature must contains 2 values, but found $result" })
        result.sortBy { it.key }
        return result[0] to result[1]
    }

    private fun combine(featureName: String, categoryName: String): String = "$featureName=$categoryName"
}