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

/**
 * @author Vitaliy.Bibaev
 */
object FactorsUtil {
    private const val COUNT = "count"
    private const val AVERAGE = "average"
    private const val VARIANCE = "variance"

    fun mergeAverage(n1: Int, avg1: Double, n2: Int, avg2: Double): Double {
        if (n1 == 0 && n2 == 0) return 0.0
        val total = (n1 + n2).toDouble()
        return (n1 / total) * avg1 + (n2 / total) * avg2
    }

    fun mergeVariance(n1: Int, variance1: Double, avg1: Double, n2: Int, variance2: Double, avg2: Double): Double {
        if (n1 == 0 && n2 == 0) return 0.0
        if (n1 == 0) return variance2
        if (n2 == 0) return variance1

        val total = (n1 + n2).toDouble()

        val rawSecondMoment1 = variance1 + avg1 * avg1
        val rawSecondMoment2 = variance2 + avg2 * avg2

        val rawSecondMoment = (n1 / total) * rawSecondMoment1 + (n2 / total) * rawSecondMoment2
        val avgSquare = with(mergeAverage(n1, avg1, n2, avg2)) { this * this }

        return rawSecondMoment - avgSquare
    }

    fun updateAverageValue(map: MutableMap<String, Double>, valueToAdd: Double) {
        val count = map[COUNT]?.toInt()
        val avg = map[AVERAGE]
        if (count != null && avg != null) {
            val newAverage = mergeAverage(1, valueToAdd, count, avg)
            updateAverage(map, 1 + count, newAverage)
        } else {
            updateAverage(map, 1, valueToAdd)
        }
    }

    fun updateAverageAndVariance(map: MutableMap<String, Double>, valueToAdd: Double) {
        fun update(count: Int, average: Double, variance: Double) = map.apply {
            this[COUNT] = count.toDouble()
            this[AVERAGE] = average
            this[VARIANCE] = variance
        }

        updateAverageValue(map, valueToAdd)
        val count = map[COUNT]
        val average = map[AVERAGE]
        val variance = map[VARIANCE]
        if (count != null && average != null && variance != null) {
            update(count.toInt() + 1,
                    mergeAverage(1, valueToAdd, count.toInt(), average),
                    mergeVariance(1, 0.0, valueToAdd, count.toInt(), variance, average))
        } else {
            update(1, valueToAdd, 0.0)
        }
    }

    fun calculateAverageByAllDays(factor: DailyAggregatedDoubleFactor): Double? {
        var totalCount = 0
        var average = 0.0
        var present = false
        for (onDate in factor.availableDays().mapNotNull { factor.onDate(it) }) {
            val avg = onDate[AVERAGE]
            val count = onDate[COUNT]?.toInt()
            if (avg != null && count != null && count > 0) {
                present = true
                average = FactorsUtil.mergeAverage(totalCount, average, count, avg)
                totalCount += count
            }
        }

        return if (present) average else null
    }

    fun calculateVarianceByAllDays(factor: DailyAggregatedDoubleFactor): Double? {
        var totalCount = 0
        var variance = 0.0
        var present = false
        var average = 0.0
        for (onDate in factor.availableDays().mapNotNull { factor.onDate(it) }) {
            val avg = onDate[VARIANCE]
            val count = onDate[COUNT]?.toInt()
            val varianceOnDate = onDate[VARIANCE]
            if (avg != null && count != null && count > 0 && varianceOnDate != null) {
                present = true
                variance = FactorsUtil.mergeVariance(totalCount, variance, average, count, varianceOnDate, avg)
                average = FactorsUtil.mergeAverage(totalCount, average, count, avg)
                totalCount += count
            }
        }

        return if (present) variance else null
    }

    private fun updateAverage(map: MutableMap<String, Double>, count: Int, avg: Double) {
        map[COUNT] = count.toDouble()
        map[AVERAGE] = avg
    }
}