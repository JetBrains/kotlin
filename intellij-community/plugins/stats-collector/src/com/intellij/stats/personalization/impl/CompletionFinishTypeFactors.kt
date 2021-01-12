// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.personalization.impl

import com.intellij.stats.personalization.FactorReader
import com.intellij.stats.personalization.FactorUpdater
import com.intellij.stats.personalization.UserFactorBase
import com.intellij.stats.personalization.UserFactorDescriptions

/**
 * @author Vitaliy.Bibaev
 */
private const val explicitSelectKey = "explicitSelect"
private const val typedSelectKey = "typedSelect"
private const val cancelledKey = "cancelled"

class CompletionFinishTypeReader(private val factor: DailyAggregatedDoubleFactor) : FactorReader {
    fun getCountByKey(key: String): Double = factor.aggregateSum()[key] ?: 0.0

    fun getTotalCount(): Double =
            getCountByKey(explicitSelectKey) + getCountByKey(typedSelectKey) + getCountByKey(cancelledKey)
}

class CompletionFinishTypeUpdater(private val factor: MutableDoubleFactor) : FactorUpdater {
    fun fireExplicitCompletionPerformed(): Boolean = factor.incrementOnToday(explicitSelectKey)
    fun fireTypedSelectPerformed(): Boolean = factor.incrementOnToday(typedSelectKey)
    fun fireLookupCancelled(): Boolean = factor.incrementOnToday(cancelledKey)

}

sealed class CompletionFinishTypeRatioBase(private val key: String)
    : UserFactorBase<CompletionFinishTypeReader>("completionFinishType${key.capitalize()}", UserFactorDescriptions.COMPLETION_FINISH_TYPE) {
    override fun compute(reader: CompletionFinishTypeReader): String? {
        val total = reader.getTotalCount()
        if (total <= 0) return null
        return (reader.getCountByKey(key) / total).toString()
    }
}

class ExplicitSelectRatio : CompletionFinishTypeRatioBase(explicitSelectKey)
class TypedSelectRatio : CompletionFinishTypeRatioBase(typedSelectKey)
class LookupCancelledRatio : CompletionFinishTypeRatioBase(cancelledKey)

