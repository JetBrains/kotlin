/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.fus.Metric
import javax.inject.Inject

abstract class BuildFlowFusStatisticsBuildService @Inject constructor(
    private val objects: ObjectFactory,
    private val providerFactory: ProviderFactory,
) : InternalGradleBuildFusStatisticsService<InternalGradleBuildFusStatisticsService.Parameter>() {

    private val configurationMetrics: ListProperty<Metric> = objects.listProperty(Metric::class.java)

    override fun getExecutionTimeMetrics(): Provider<List<Metric>> {
        return providerFactory.provider {
            val reportedMetrics = ArrayList<Metric>()
            reportedMetrics.addAll(executionTimeMetrics)
            reportedMetrics
        }
    }

    fun getConfigurationReportedMetrics(): Provider<List<Metric>> {
        return providerFactory.provider {
            synchronized(this) {
                configurationMetrics.disallowChanges()
                configurationMetrics.get()
            }
        }
    }

    fun collectMetrics(metrics: Provider<List<Metric>>) {
        synchronized(this) {
            configurationMetrics.addAll(metrics)
        }
    }

    fun collectMetric(metric: Provider<Metric>) {
        synchronized(this) {
            configurationMetrics.add(metric)
        }
    }

    override fun close() {
        // since Gradle 8.1 flow action [BuildFinishFlowAction] is used to collect all metrics and write them down in a single file
    }
}