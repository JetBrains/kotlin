/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.plugin.StatisticsBuildFlowManager
import javax.inject.Inject

abstract class FlowActionBuildFusService @Inject constructor(
    private val objects: ObjectFactory,
    private val providerFactory: ProviderFactory,
) : BuildFusService<BuildFusService.Parameters>() {
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

    private val configurationMetrics: ListProperty<MetricContainer> = objects.listProperty(MetricContainer::class.java)

    fun addConfigurationTimeMetric(metric: Provider<MetricContainer>) {
        synchronized(this) {
            configurationMetrics.add(metric)
        }
    }

    fun addConfigurationTimeMetric(metric: MetricContainer) {
        synchronized(this) {
            configurationMetrics.add(metric)
        }
    }

    fun addConfigurationTimeMetrics(metrics: List<MetricContainer>) {
        providerFactory.provider {
            synchronized(this) {
                configurationMetrics.addAll(metrics)
            }
        }
    }

    fun getConfigurationTimeMetrics(): Provider<List<MetricContainer>> {
        return providerFactory.provider {
            synchronized(this) {
                configurationMetrics.disallowChanges()
                configurationMetrics.get()
            }
        }
    }
}