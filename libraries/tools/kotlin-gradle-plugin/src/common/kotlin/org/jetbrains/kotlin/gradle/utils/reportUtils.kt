/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.MetricContainer
import org.jetbrains.kotlin.statistics.MetricValueValidationFailed


/**
 * Invokes build FUS service if the reporting service is initialised and add configuration time metrics.
 *
 * New value will be present in configuration cache if BuildFusService.Parameters are not calculated yet
 * [BuildFusService.reportFusMetrics] should be called for execution time metrics
 */
internal fun Project.addConfigurationMetrics(reportAction: (MetricContainer) -> Unit) {
    project.gradle.sharedServices.registrations.findByName(BuildFusService.serviceName)?.also {
        with(it.parameters as BuildFusService.Parameters) {
            configurationMetrics.add(project.provider {
                val configurationTimeMetrics = MetricContainer()
                reportAction(configurationTimeMetrics)
                configurationTimeMetrics
            })
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
