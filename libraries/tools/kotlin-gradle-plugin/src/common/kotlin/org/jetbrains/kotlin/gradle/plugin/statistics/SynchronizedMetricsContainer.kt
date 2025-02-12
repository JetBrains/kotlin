/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.statistics.metrics.*
import java.io.Serializable

internal class SynchronizedMetricsContainer : NonSynchronizedMetricsContainer(), Serializable {

    private val metricsLock = Object()

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
        synchronized(metricsLock) {
            return super.report(metric, value, subprojectName, weight)
        }
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean {
        synchronized(metricsLock) {
            return super.report(metric, value, subprojectName, weight)
        }
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean {
        synchronized(metricsLock) {
            return super.report(metric, value, subprojectName, weight)
        }
    }

    override fun readFromMetricConsumer(metricConsumer: NonSynchronizedMetricsContainer) {
        synchronized(metricsLock) {
            super.readFromMetricConsumer(metricConsumer)
        }
    }

    override fun sendToConsumer(metricConsumer: StatisticsValuesConsumer) {
        synchronized(metricsLock) {
            super.sendToConsumer(metricConsumer)
        }
    }
}