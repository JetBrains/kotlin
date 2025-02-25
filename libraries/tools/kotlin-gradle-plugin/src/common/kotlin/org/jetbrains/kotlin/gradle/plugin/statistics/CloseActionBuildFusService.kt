/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.fus.BuildUidService

abstract class CloseActionBuildFusService:
    BuildFusService<ConfigurationMetricsBuildFusParameters>() {

    companion object {
        internal fun registerIfAbsentImpl(
            project: Project,
            buildUidService: Provider<BuildUidService>,
            generalConfigurationMetricsProvider: Provider<MetricContainer>,
        ): Provider<CloseActionBuildFusService> {
            return project.gradle.sharedServices.registerIfAbsent(serviceName, CloseActionBuildFusService::class.java) { spec ->
                spec.parameters.generalConfigurationMetrics.set(generalConfigurationMetricsProvider)
                spec.parameters.generalMetricsFinalized.set(false)
                spec.parameters.buildStatisticsConfiguration.set(KotlinBuildStatsConfiguration(project))
                spec.parameters.buildId.value(buildUidService.map { it.buildId }).disallowChanges()
                //init value to avoid `java.lang.IllegalStateException: GradleScopeServices has been closed` exception on close
                spec.parameters.configurationMetrics.add(MetricContainer())
            }
        }
    }

    override fun close() {
        recordBuildFinished(buildFailed, buildId, parameters.configurationMetrics.orElse(emptyList()).get())
        super.close()
    }
}