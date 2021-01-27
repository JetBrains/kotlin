/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

interface StatsCollector {
    data class StatsKey(
        val id: String,
        val extensionReceiver: String?,
        val parameterNames: List<String>,
        val parameterTypes: List<String>,
        val declarationType: DeclarationType
    ) {
        constructor(id: String, declarationType: DeclarationType) : this(id, null, emptyList(), emptyList(), declarationType)
    }

    fun logDeclaration(targetIndex: Int, lazyStatsKey: () -> StatsKey)
    fun writeTo(statsOutput: StatsOutput)
}
