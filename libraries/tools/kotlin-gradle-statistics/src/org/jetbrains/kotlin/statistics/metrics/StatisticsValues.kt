/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.metrics

interface ReportStatisticsValue<T> {
    val name: String
    val value: T
}

class ReportOnceStatisticsValue<T>(override val name: String, override val value: T) :
    ReportStatisticsValue<T>

interface AdditiveStatisticsValue<T> : ReportStatisticsValue<T> {
    fun addValue(t: T)
}

interface IStatisticsValuesConsumer {

    fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String? = null)

    fun report(metric: NumericalMetrics, value: Long, subprojectName: String? = null)

    fun report(metric: StringMetrics, value: String, subprojectName: String? = null)

}