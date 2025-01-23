/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheEnabled
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import javax.inject.Inject

abstract class CloseActionBuildFusService @Inject constructor(private val objects: ObjectFactory) :
    BuildFusService<CloseActionBuildFusService.Parameters>(objects) {

    interface Parameters : BuildFusService.Parameters {
        val configurationMetrics: ListProperty<MetricContainer>
    }

    companion object {
        internal fun registerIfAbsentImpl(
            project: Project,
            buildUidService: Provider<BuildUidService>,
            generalConfigurationMetricsProvider: Provider<MetricContainer>,
        ): Provider<out BuildFusService<out BuildFusService.Parameters>> {
            return project.gradle.sharedServices.registerIfAbsent(serviceName, CloseActionBuildFusService::class.java) { spec ->
                spec.parameters.generalConfigurationMetrics.set(generalConfigurationMetricsProvider)
                spec.parameters.buildStatisticsConfiguration.set(KotlinBuildStatsConfiguration(project))
                spec.parameters.buildId.value(buildUidService.map { it.buildId }).disallowChanges()
                //init value to avoid `java.lang.IllegalStateException: GradleScopeServices has been closed` exception on close
                spec.parameters.configurationMetrics.add(MetricContainer())
            }.also { buildService ->
                //DO NOT call buildService.get() before all parameters.configurationMetrics are set.
                // buildService.get() call will cause parameters calculation and configuration cache storage.

                //Gradle throws an exception when Gradle version less than 7.4 with configuration cache enabled and buildSrc,
                @Suppress("DEPRECATION")
                if (GradleVersion.current().baseVersion >= GradleVersion.version("7.4")
                    || !project.isConfigurationCacheEnabled
                    || project.currentBuildId().name != "buildSrc"
                ) {
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(buildService)
                }
            }
        }
    }


    override fun addConfigurationTimeMetric(metric: MetricContainer) {
        synchronized(this) {
            parameters.configurationMetrics.add(metric)
        }
    }

    override fun addConfigurationTimeMetrics(metrics: List<MetricContainer>) {
        synchronized(this) {
            parameters.configurationMetrics.addAll(metrics)
        }
    }

    override fun getConfigurationTimeMetrics(): Provider<List<MetricContainer>> {
        return synchronized(this) {
            parameters.configurationMetrics.disallowChanges()
            parameters.configurationMetrics
        }
    }

    override fun addConfigurationTimeMetric(metric: Provider<MetricContainer>) {
        synchronized(this) {
            parameters.configurationMetrics.add(metric)
        }
    }

    override fun close() {
        recordBuildFinished(buildFailed, buildId, emptyList())
        super.close()
    }
}