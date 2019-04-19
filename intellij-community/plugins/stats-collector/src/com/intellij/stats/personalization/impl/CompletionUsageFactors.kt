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

/**
 * @author Vitaliy.Bibaev
 */
class CompletionUsageReader(factor: DailyAggregatedDoubleFactor) : UserFactorReaderBase(factor) {
    fun getTodayCount(): Double = factor.onToday().getOrDefault("count", 0.0)

    fun getTotalCount(): Double = factor.aggregateSum().getOrDefault("count", 0.0)

    fun getWeekAverage(): Double = factor.aggregateAverage().getOrDefault("count", 0.0)
}

class CompletionUsageUpdater(factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun fireCompletionUsed() {
        factor.incrementOnToday("count")
    }
}

class TodayCompletionUsageCount : CompletionUsageFactorBase("todayCompletionCount") {
    override fun compute(reader: CompletionUsageReader): Double? = reader.getTodayCount()
}

class WeekAverageUsageCount : CompletionUsageFactorBase("weekAverageDailyCompletionCount") {
    override fun compute(reader: CompletionUsageReader): Double? = reader.getWeekAverage()
}

class TotalUsageCount : CompletionUsageFactorBase("totalCompletionCountInLastDays") {
    override fun compute(reader: CompletionUsageReader): Double? = reader.getTotalCount()
}

abstract class CompletionUsageFactorBase(override val id: String) : UserFactor {
    final override fun compute(storage: UserFactorStorage): String? =
            compute(storage.getFactorReader(UserFactorDescriptions.COMPLETION_USAGE))?.toString()

    abstract fun compute(reader: CompletionUsageReader): Double?
}
