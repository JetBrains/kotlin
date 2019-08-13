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
class PrefixLengthReader(factor: DailyAggregatedDoubleFactor) : UserFactorReaderBase(factor) {
    fun getCountsByPrefixLength(): Map<Int, Double> {
        return factor.aggregateSum().asIterable().associate { (key, value) -> key.toInt() to value }
    }

    fun getAveragePrefixLength(): Double? {
        val lengthToCount = getCountsByPrefixLength()
        if (lengthToCount.isEmpty()) return null

        val totalChars = lengthToCount.asSequence().sumByDouble { it.key * it.value }
        val completionCount = lengthToCount.asSequence().sumByDouble { it.value }

        if (completionCount == 0.0) return null
        return totalChars / completionCount
    }
}

class PrefixLengthUpdater(factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun fireCompletionPerformed(prefixLength: Int) {
        factor.incrementOnToday(prefixLength.toString())
    }
}

class MostFrequentPrefixLength : UserFactorBase<PrefixLengthReader>("mostFrequentPrefixLength",
        UserFactorDescriptions.PREFIX_LENGTH_ON_COMPLETION) {
    override fun compute(reader: PrefixLengthReader): String? {
        return reader.getCountsByPrefixLength().maxBy { it.value }?.key?.toString()
    }
}

class AveragePrefixLength : UserFactorBase<PrefixLengthReader>("", UserFactorDescriptions.PREFIX_LENGTH_ON_COMPLETION) {
    override fun compute(reader: PrefixLengthReader): String? {
        return reader.getAveragePrefixLength()?.toString()
    }
}