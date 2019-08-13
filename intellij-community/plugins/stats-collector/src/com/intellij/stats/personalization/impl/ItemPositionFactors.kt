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

/**
 * @author Vitaliy.Bibaev
 */
class ItemPositionReader(factor: DailyAggregatedDoubleFactor) : UserFactorReaderBase(factor) {
    fun getCountsByPosition(): Map<Int, Double> {
        return factor.aggregateSum().asIterable().associate { (key, value) -> key.toInt() to value }
    }

    fun getAveragePosition(): Double? {
        val positionToCount = getCountsByPosition()
        if (positionToCount.isEmpty()) return null

        val positionsSum = positionToCount.asSequence().sumByDouble { it.key * it.value }
        val completionCount = positionToCount.asSequence().sumByDouble { it.value }

        if (completionCount == 0.0) return null
        return positionsSum / completionCount
    }
}

class ItemPositionUpdater(factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun fireCompletionPerformed(selectedItemOrder: Int) {
        factor.incrementOnToday(selectedItemOrder.toString())
    }
}

class AverageSelectedItemPosition
    : UserFactorBase<ItemPositionReader>("averageSelectedPosition", UserFactorDescriptions.SELECTED_ITEM_POSITION) {
    override fun compute(reader: ItemPositionReader): String? = reader.getAveragePosition()?.toString()
}

class MaxSelectedItemPosition
    : UserFactorBase<ItemPositionReader>("maxSelectedItemPosition", UserFactorDescriptions.SELECTED_ITEM_POSITION) {
    override fun compute(reader: ItemPositionReader): String? =
            reader.getCountsByPosition().asSequence().filter { it.value != 0.0 }.maxBy { it.key }?.key?.toString()
}

class MostFrequentSelectedItemPosition
    : UserFactorBase<ItemPositionReader>("mostFrequentItemPosition", UserFactorDescriptions.SELECTED_ITEM_POSITION) {
    override fun compute(reader: ItemPositionReader): String? =
            reader.getCountsByPosition().maxBy { it.value }?.key?.toString()
}

