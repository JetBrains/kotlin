/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.fus.internal.GradleBuildFusStatisticsBuildService
import org.jetbrains.kotlin.gradle.fus.internal.GradleBuildFusStatisticsBuildService.Companion.serviceName
import org.jetbrains.kotlin.gradle.fus.internal.InternalGradleBuildFusStatisticsService

/**
 * Invokes [GradleBuildFusStatisticsBuildService] if the reporting service is initialized and add configuration time metrics.
 *
 * New value will be present in configuration cache if GradleBuildFusStatisticsBuildService.Parameters are not calculated yet
 * [GradleBuildFusStatisticsBuildService.reportMetric] should be called for execution time metrics
 */
fun Project.addConfigurationMetric(reportAction: () -> Map<String, Any>) {
    project.gradle.sharedServices.registrations.findByName(serviceName)?.also {
        val parameters = it.parameters
        if (parameters is InternalGradleBuildFusStatisticsService.Parameters) {
            parameters.configurationMetrics.add(project.provider {
                reportAction()
            })
        }
    }
}