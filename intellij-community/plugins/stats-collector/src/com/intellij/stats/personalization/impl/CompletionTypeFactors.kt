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

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.stats.personalization.*

/**
 * @author Vitaliy.Bibaev
 */
class CompletionTypeReader(private val factor: DailyAggregatedDoubleFactor) : FactorReader {
    fun getCompletionCountByType(type: CompletionType): Double =
            factor.aggregateSum().getOrDefault(type.toString(), 0.0)

    fun getTotalCompletionCount(): Double = factor.aggregateSum().values.sum()
}

class CompletionTypeUpdater(private val factor: MutableDoubleFactor) : FactorUpdater {
    fun fireCompletionPerformed(type: CompletionType) {
        factor.incrementOnToday(type.toString())
    }
}

class CompletionTypeRatio(private val type: CompletionType) : UserFactor {

    override val id: String = "CompletionTypeRatioOf$type"
    override fun compute(storage: UserFactorStorage): String? {
        val reader = storage.getFactorReader(UserFactorDescriptions.COMPLETION_TYPE)
        val total = reader.getTotalCompletionCount()
        return if (total == 0.0) "0.0" else (reader.getCompletionCountByType(type) / total).toString()
    }
}
