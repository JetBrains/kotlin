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
                             override val undefinedIndex: Int,
                             override val otherCategoryIndex: Int,
                             private val categoryToIndex: Map<String, Int>)
    : CategoricalFeature {
    override fun indexByCategory(category: String): Int = categoryToIndex[category] ?: otherCategoryIndex

    override val categories: Set<String> = categoryToIndex.keys

    override fun process(value: Any, featureArray: DoubleArray) {
        setDefaults(featureArray)
        featureArray[indexByCategory(value.toString())] = 1.0
        featureArray[undefinedIndex] = 0.0
    }

    override fun setDefaults(featureArray: DoubleArray) {
        categories.forEach { featureArray[indexByCategory(it)] = 0.0 }
        featureArray[undefinedIndex] = 1.0
        featureArray[otherCategoryIndex] = 0.0
    }
}