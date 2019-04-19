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
class TimeBetweenTypingReader(factor: DailyAggregatedDoubleFactor) : UserFactorReaderBase(factor) {
    fun averageTime(): Double? {
        return FactorsUtil.calculateAverageByAllDays(factor)
    }
}

class TimeBetweenTypingUpdater(factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun fireTypingPerformed(delayMs: Int) {
        factor.updateOnDate(DateUtil.today()) {
            FactorsUtil.updateAverageValue(this, delayMs.toDouble())
        }
    }
}

class AverageTimeBetweenTyping
    : UserFactorBase<TimeBetweenTypingReader>("averageTimeBetweenTyping", UserFactorDescriptions.TIME_BETWEEN_TYPING) {
    override fun compute(reader: TimeBetweenTypingReader): String? = reader.averageTime()?.toString()
}