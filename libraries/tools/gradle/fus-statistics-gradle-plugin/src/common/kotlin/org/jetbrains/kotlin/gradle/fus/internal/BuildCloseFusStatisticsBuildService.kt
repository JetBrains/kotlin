/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.fus.Metric
import javax.inject.Inject

/**
 * Build close action is used for Gradle versions up to 8.1, For older version [BuildFinishFlowAction] is used.
 */
internal abstract class BuildCloseFusStatisticsBuildService @Inject constructor(private val providersFactor: ProviderFactory) :
    InternalGradleBuildFusStatisticsService<BuildCloseFusStatisticsBuildService.Parameter>(), AutoCloseable {

    internal interface Parameter : InternalGradleBuildFusStatisticsService.Parameter {
        val configurationMetrics: ListProperty<Metric>
    }

    private val log = Logging.getLogger(this.javaClass)

    override fun getExecutionTimeMetrics(): Provider<List<Metric>> {
        return providersFactor.provider {
            val reportedMetrics = ArrayList<Metric>()

            parameters.configurationMetrics.map {
                reportedMetrics.addAll(it)
            }
            reportedMetrics.addAll(executionTimeMetrics)
            reportedMetrics
        }

    }

    override fun close() {
        writeDownFusMetrics(parameters.configurationMetrics.orNull)
        super.close()
    }
}