/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.ConfigurationMetricsBuildFusParameters
import org.jetbrains.kotlin.gradle.plugin.statistics.FlowActionBuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.MetricContainer
import org.jetbrains.kotlin.statistics.MetricValueValidationFailed


/**
 * Invokes build FUS service if the reporting service is initialized and add configuration time metrics.
 *
 * New value will be present in configuration cache if BuildFusService.Parameters are not calculated yet
 * [BuildFusService.reportFusMetrics] should be called for execution time metrics
 */
internal fun Project.addConfigurationMetrics(reportAction: (MetricContainer) -> Unit) {
    addConfigurationMetrics(
        project.provider {
            val configurationTimeMetrics = MetricContainer()
            reportAction(configurationTimeMetrics)
            configurationTimeMetrics
        }
    )
}

private fun Project.addConfigurationMetrics(metricContainer: Provider<MetricContainer>) {
    project.gradle.sharedServices.registrations.findByName(BuildFusService.serviceName)?.also {
        val parameters = it.parameters
        if (parameters is ConfigurationMetricsBuildFusParameters) {
            //build service parameter is used,
            //it is important to avoid service parameters initialization before all configuration metrics are collected
            parameters.configurationMetrics.add(metricContainer)
        } else {
            //build service field is used,
            //it is safe to access build service, as configuration metrics will be cached in [BuildFinishFlowAction]
            val buildFusService = it.service.orNull as FlowActionBuildFusService
            buildFusService.addConfigurationTimeMetric(metricContainer)
        }
    }
}

internal fun <T> runMetricMethodSafely(logger: Logger, methodName: String, action: () -> T?): T? {
    return try {
        logger.debug("Executing [$methodName]")
        action.invoke()
    } catch (e: MetricValueValidationFailed) {
        throw e
    } catch (e: Throwable) {
        val description = "Could not execute [$methodName]"
        logger.info(description)
        logger.debug(e.message, e)
        null
    }
}
