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

package com.intellij.stats.personalization.impl

import com.intellij.stats.personalization.UserFactorBase
import com.intellij.stats.personalization.UserFactorDescriptions
import com.intellij.stats.personalization.UserFactorReaderBase
import com.intellij.stats.personalization.UserFactorUpdaterBase
import com.jetbrains.completion.feature.CategoricalFeature
import com.jetbrains.completion.feature.impl.FeatureUtils

/**
 * @author Vitaliy.Bibaev
 */
class CategoryFeatureReader(factor: DailyAggregatedDoubleFactor)
    : UserFactorReaderBase(factor) {
    fun calculateRatioByValue(): Map<String, Double> {
        val sums = factor.aggregateSum()
        val total = sums.values.sum()
        if (total == 0.0) return emptyMap()
        return sums.mapValues { e -> e.value / total }
    }
}

class CategoryFeatureUpdater(private val knownCategories: Set<String>, factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun update(value: Any?) {
        if (value == null) {
            factor.incrementOnToday(FeatureUtils.UNDEFINED)
        } else {
            val category = value.toString()
            if (category in knownCategories) {
                factor.incrementOnToday(category)
            } else {
                factor.incrementOnToday(FeatureUtils.OTHER)
            }
        }
    }
}

class CategoryRatio(feature: CategoricalFeature, private val categoryName: String)
    : UserFactorBase<CategoryFeatureReader>("cat:${feature.name}:${categoryName}",
                                            UserFactorDescriptions.categoricalFeatureDescriptor(feature)) {
    override fun compute(reader: CategoryFeatureReader): String {
        return reader.calculateRatioByValue().getOrDefault(categoryName, -1.0).toString()
    }
}

class MostFrequentCategory(feature: CategoricalFeature)
    : UserFactorBase<CategoryFeatureReader>("mostFrequent:${feature.name}",
        UserFactorDescriptions.categoricalFeatureDescriptor(feature)) {
    override fun compute(reader: CategoryFeatureReader): String? {
        return reader.calculateRatioByValue().maxBy { it.value }?.key
    }
}
