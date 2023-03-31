/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

interface IStatisticsValuesConsumer {
    fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String? = null, weight: Long? = null): Boolean

    fun report(metric: NumericalMetrics, value: Long, subprojectName: String? = null, weight: Long? = null): Boolean

    fun report(metric: StringMetrics, value: String, subprojectName: String? = null, weight: Long? = null): Boolean
}
