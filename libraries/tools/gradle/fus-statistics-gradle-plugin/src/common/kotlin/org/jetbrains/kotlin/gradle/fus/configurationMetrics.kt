/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.fus.internal.CommonFusServiceParameters
import org.jetbrains.kotlin.gradle.fus.internal.serviceName

/**
 * Invokes [GradleBuildFusStatisticsService] if the reporting service is initialized and add configuration time metrics.
 *
 * New value will be present in configuration cache if GradleBuildFusStatisticsBuildService.Parameters are not calculated yet
 * [GradleBuildFusStatisticsService.reportMetric] should be called for execution time metrics
 */
fun Project.addGradleConfigurationPhaseMetric(reportAction: () -> Collection<Metric>) {
    project.gradle.sharedServices.registrations.findByName(serviceName)?.also { fusService ->
        val parameters = fusService.parameters
        if (parameters is CommonFusServiceParameters) {
            parameters.configurationMetrics.addAll(project.provider {
                reportAction()
            })
            fusService.parameters
        }
    }
}