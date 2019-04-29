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

import com.intellij.stats.personalization.*
import com.jetbrains.completion.feature.DoubleFeature
import com.jetbrains.completion.feature.impl.FeatureUtils

/**
 * @author Vitaliy.Bibaev
 */
class DoubleFeatureReader(factor: DailyAggregatedDoubleFactor)
    : UserFactorReaderBase(factor) {
    fun calculateAverageValue(): Double? = FactorsUtil.calculateAverageByAllDays(factor)
    fun calculateVariance(): Double? = FactorsUtil.calculateVarianceByAllDays(factor)
    fun min(): Double? = factor.aggregateMin()["min"]
    fun max(): Double? = factor.aggregateMax()["max"]

    fun undefinedRatio(): Double? {
        val sums = factor.aggregateSum()
        val total = sums["count"] ?: return null
        if (total == 0.0) return null

        return sums.getOrDefault(FeatureUtils.UNDEFINED, 0.0) / total
    }
}

class DoubleFeatureUpdater(factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun update(value: Any?) {
        if (value == null || "null" == value) {
            factor.incrementOnToday(FeatureUtils.UNDEFINED)
        } else {
            val doubleValue = value.asDouble()
            factor.updateOnDate(DateUtil.today()) {
                FactorsUtil.updateAverageAndVariance(this, doubleValue)
                compute("max", { _, old -> if (old == null) doubleValue else maxOf(old, doubleValue) })
                compute("min", { _, old -> if (old == null) doubleValue else minOf(old, doubleValue) })
            }
        }
    }

    private fun Any.asDouble(): Double {
        if (this is Number) return this.toDouble()
        return this.toString().toDouble()
    }
}

abstract class DoubleFeatureUserFactorBase(prefix: String, feature: DoubleFeature) :
        UserFactorBase<DoubleFeatureReader>("${prefix}Double:${feature.name}$",
                UserFactorDescriptions.doubleFeatureDescriptor(feature))

class AverageDoubleFeatureValue(feature: DoubleFeature) : DoubleFeatureUserFactorBase("avg", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.calculateAverageValue()?.toString()
}

class MinDoubleFeatureValue(feature: DoubleFeature) : DoubleFeatureUserFactorBase("min", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.min()?.toString()
}

class MaxDoubleFeatureValue(feature: DoubleFeature) : DoubleFeatureUserFactorBase("max", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.max()?.toString()
}

class UndefinedDoubleFeatureValueRatio(feature: DoubleFeature) : DoubleFeatureUserFactorBase("undefinedRatio", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.undefinedRatio()?.toString()
}

class VarianceDoubleFeatureValue(feature: DoubleFeature) : DoubleFeatureUserFactorBase("variance", feature) {
    override fun compute(reader: DoubleFeatureReader): String? = reader.calculateVariance()?.toString()
}