/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.fus.internal.SynchronizedConfigurationMetrics
import org.jetbrains.kotlin.gradle.plugin.StatisticsBuildFlowManager
import javax.inject.Inject

abstract class FlowActionBuildFusService @Inject constructor(
    objects: ObjectFactory,
    private val providerFactory: ProviderFactory,
) : BuildFusService<BuildFusService.Parameters>() {
    private val logger: Logger = Logging.getLogger(this.javaClass)

    init {
        logger.debug("${this.javaClass.simpleName} ${this.hashCode()} is created")
    }

    companion object {
        internal fun registerIfAbsentImpl(
            project: Project,
            buildUidService: Provider<BuildUidService>,
            generalConfigurationMetricsProvider: Provider<MetricContainer>,
        ): Provider<out BuildFusService<out Parameters>> {
            return project.gradle.sharedServices.registerIfAbsent(serviceName, FlowActionBuildFusService::class.java) { spec ->
                spec.parameters.generalConfigurationMetrics.set(generalConfigurationMetricsProvider)
                spec.parameters.buildStatisticsConfiguration.set(KotlinBuildStatsConfiguration(project))
                spec.parameters.buildId.value(buildUidService.map { it.buildId }).disallowChanges()
            }.also { buildService ->
                StatisticsBuildFlowManager.getInstance(project).subscribeForBuildResultAndConfigurationTimeMetrics(buildService)
            }
        }
    }

    private val configurationMetrics = SynchronizedConfigurationMetrics(objects.listProperty(MetricContainer::class.java), logger)

    fun addConfigurationTimeMetric(metric: Provider<MetricContainer>) {
        configurationMetrics.add(metric)
    }

    fun addConfigurationTimeMetric(metric: MetricContainer) {
        configurationMetrics.add(metric)
    }

    fun addConfigurationTimeMetrics(metrics: List<MetricContainer>) {
        providerFactory.provider {
            configurationMetrics.addAll(metrics)
        }
    }

    fun getConfigurationTimeMetrics(): Provider<List<MetricContainer>> {
        return providerFactory.provider {
            configurationMetrics.getConfigurationMetrics()
        }
    }

    override fun close() {
        super.close()
        logger.debug("${this.javaClass.simpleName} ${this.hashCode()} is closed")
    }
}