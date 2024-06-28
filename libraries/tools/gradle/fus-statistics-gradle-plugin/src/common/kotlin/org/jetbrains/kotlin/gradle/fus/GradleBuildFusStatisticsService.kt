/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal


/**
 * A service interface for build FUS statistics reporting.
 */
interface GradleBuildFusStatisticsService {

    /**
     * Reports a metric by its name and optionally subproject.
     *
     * @param name the metric name
     * @param value  the metric value.
     * @param subprojectName the subproject name for which the metric is being reported.
     */
    fun reportMetric(name: String, value: String, subprojectName: String? = null)

    /**
     * @see org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService.reportMetric(java.lang.String, java.lang.String, java.lang.String)
     */
    fun reportMetric(name: String, value: Number, subprojectName: String? = null)

    /**
     * @see org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService.reportMetric(java.lang.String, java.lang.String, java.lang.String)
     */
    fun reportMetric(name: String, value: Boolean, subprojectName: String? = null)

}

/**
 * This task interface provide access to GradleBuildFusStatisticsService.
 *
 * @property fusStatisticsBuildService holds an instance of GradleBuildFusStatisticsService.
 */
interface UsesGradleBuildFusStatisticsService : Task {
    @get:Internal
    val fusStatisticsBuildService: Property<GradleBuildFusStatisticsService>
}