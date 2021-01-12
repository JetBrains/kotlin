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
class MnemonicsUsageReader(factor: DailyAggregatedDoubleFactor) : UserFactorReaderBase(factor) {
    fun mnemonicsUsageRatio(): Double? {
        val sums = factor.aggregateSum()
        val total = sums["total"]
        val used = sums["withMnemonics"]
        if (total == null || used == null || total < 1.0) return null
        return used / total
    }
}

class MnemonicsUsageUpdater(factor: MutableDoubleFactor) : UserFactorUpdaterBase(factor) {
    fun fireCompletionFinished(isMnemonicsUsed: Boolean) {
        factor.updateOnDate(DateUtil.today()) {
            compute("total", { _, before -> if (before == null) 1.0 else before + 1 })
            val valueBefore = computeIfAbsent("withMnemonics", { 0.0 })
            if (isMnemonicsUsed) {
                set("withMnemonics", valueBefore + 1.0)
            }
        }
    }
}

class MnemonicsRatio : UserFactorBase<MnemonicsUsageReader>("mnemonicsUsageRatio", UserFactorDescriptions.MNEMONICS_USAGE) {
    override fun compute(reader: MnemonicsUsageReader): String? = reader.mnemonicsUsageRatio()?.toString()
}