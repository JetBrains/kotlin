/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.kotlin.gradle.fus.Metric
import javax.inject.Inject

abstract class BuildFlowFusStatisticsBuildService @Inject constructor(
    objects: ObjectFactory,
    private val providerFactory: ProviderFactory,
) : InternalGradleBuildFusStatisticsService<InternalGradleBuildFusStatisticsService.Parameter>(),
    OperationCompletionListener {
    private val logger: Logger = Logging.getLogger(this.javaClass)
    private val configurationMetrics = SynchronizedConfigurationMetrics(objects.listProperty(Metric::class.java), logger)

    init {
        logger.debug("${this.javaClass.simpleName} ${this.hashCode()} is created")
    }

    override fun getExecutionTimeMetrics(): Provider<List<Metric>> {
        return providerFactory.provider {
            val reportedMetrics = ArrayList<Metric>()
            reportedMetrics.addAll(executionTimeMetrics)
            reportedMetrics
        }
    }

    fun getConfigurationReportedMetrics(): Provider<List<Metric>> {
        //important for Gradle 9.0+ with configuration cache enabled
        return providerFactory.provider {
            configurationMetrics.getConfigurationMetrics()
        }
    }

    fun collectMetrics(metrics: Provider<List<Metric>>) {
        configurationMetrics.addAll(metrics)
    }

    fun collectMetric(metric: Provider<Metric>) {
        configurationMetrics.add(metric)
    }

    override fun onFinish(p0: FinishEvent?) {
        //ignore
    }
}