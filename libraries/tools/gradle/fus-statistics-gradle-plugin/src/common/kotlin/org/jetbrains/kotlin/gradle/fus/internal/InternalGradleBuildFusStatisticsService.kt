/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.Metric
import org.jetbrains.kotlin.gradle.fus.UniqueId
import java.util.concurrent.ConcurrentLinkedQueue

abstract class InternalGradleBuildFusStatisticsService<T : InternalGradleBuildFusStatisticsService.Parameter> :
    GradleBuildFusStatisticsService<T> {

    interface Parameter : BuildServiceParameters {
        val fusStatisticsRootDirPath: Property<String>
    }

    internal val executionTimeMetrics = ConcurrentLinkedQueue<Metric>()

    override fun reportMetric(name: String, value: Boolean, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    override fun reportMetric(name: String, value: String, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    override fun reportMetric(name: String, value: Number, uniqueId: UniqueId) {
        internalReportMetric(name, value, uniqueId)
    }

    private fun internalReportMetric(name: String, value: Any, uniqueId: UniqueId) {
        //all aggregations should be done on IDEA side
        executionTimeMetrics.add(Metric(name, value, uniqueId))
    }

    /**
     * Returns a list of collected metrics sets.
     *
     *
     * These sets are not going to be merged into one as no aggregation information is present here.
     * Non-thread safe
     */
    abstract fun getExecutionTimeMetrics(): Provider<List<Metric>>

}