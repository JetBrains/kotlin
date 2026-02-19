/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal


/**
 * A service interface for build FUS statistics reporting.
 */
interface GradleBuildFusStatisticsService<T : BuildServiceParameters> : BuildService<T>, AutoCloseable {

    /**
     * Reports an execution time metric by its name and optionally subproject.
     *
     * @param name the metric name
     * @param value  the metric value.
     * @param uniqueId identification for a place where the metric is reported.
     */
    fun reportMetric(name: String, value: String, uniqueId: UniqueId = UniqueId.DEFAULT)

    /**
     * @see org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService.reportMetric(java.lang.String, java.lang.String, java.lang.String)
     */
    fun reportMetric(name: String, value: Number, uniqueId: UniqueId = UniqueId.DEFAULT)

    /**
     * @see org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService.reportMetric(java.lang.String, java.lang.String, java.lang.String)
     */
    fun reportMetric(name: String, value: Boolean, uniqueId: UniqueId = UniqueId.DEFAULT)

}

/**
 * This task interface provide access to GradleBuildFusStatisticsService.
 *
 * @property fusStatisticsBuildService holds an instance of GradleBuildFusStatisticsService.
 */
interface UsesGradleBuildFusStatisticsService : Task {
    @get:Internal
    val fusStatisticsBuildService: Property<GradleBuildFusStatisticsService<out BuildServiceParameters>>
}