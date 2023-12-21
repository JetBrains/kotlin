/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.plugin.statistics.MetricContainer


/**
 * Invokes build FUS service if the reporting service is initialised and add configuration time metrics.
 *
 * BuildFusService.reportFusMetrics should be called for execution time metrics
 */
internal fun Project.addConfigurationMetrics(reportAction: (MetricContainer) -> Unit) {
    project.gradle.sharedServices.registrations.findByName(BuildFusService.serviceName)?.also {
        @Suppress("UNCHECKED_CAST")
        (it.service as Provider<BuildFusService>).also {
            it.get().parameters.configurationMetrics.add(project.provider {
                val configurationTimeMetrics = MetricContainer()
                reportAction(configurationTimeMetrics)
                configurationTimeMetrics
            })
        }
    }
}