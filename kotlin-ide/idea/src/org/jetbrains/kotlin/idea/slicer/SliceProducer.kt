/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.findUsages.handlers.SliceUsageProcessor

interface SliceProducer {
    fun produce(usage: UsageInfo, behaviour: KotlinSliceUsage.SpecialBehaviour?, parent: SliceUsage): Collection<SliceUsage>?

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

fun SliceProducer.produceAndProcess(
    sliceUsage: SliceUsage,
    behaviour: KotlinSliceUsage.SpecialBehaviour?,
    parentUsage: SliceUsage,
    processor: SliceUsageProcessor
): Boolean {
    val result = produce(sliceUsage.usageInfo, behaviour, parentUsage) ?: listOf(sliceUsage)
    for (usage in result) {
        if (!processor.process(usage)) return false
    }
    return true
}