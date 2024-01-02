/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.PropertiesBuildService
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.gradle.utils.runMetricMethodSafely
import java.io.File
import java.io.Serializable

class KotlinBuildStatsConfiguration(
    internal val forcePropertiesValidation: Boolean,
    internal val sessionLoggerRootPath: File,
) : Serializable {
    companion object {
        // Property used for tests.
        private const val CUSTOM_LOGGER_ROOT_PATH = "kotlin.session.logger.root.path"

        // Property used for tests. Build will fail fast if collected value doesn't fit regexp
        private const val FORCE_VALUES_VALIDATION = "kotlin_performance_profile_force_validation"
        private val logger = Logging.getLogger(KotlinBuildStatsConfiguration::class.java)
    }

    constructor(project: Project) : this(
        PropertiesBuildService.registerIfAbsent(project).get().get(FORCE_VALUES_VALIDATION, project)?.toBoolean() ?: false,
        PropertiesBuildService.registerIfAbsent(project).get().get(CUSTOM_LOGGER_ROOT_PATH, project)
            ?.also {
                logger.warn("$CUSTOM_LOGGER_ROOT_PATH property for test purpose only")
            }?.let { File(it) } ?: project.gradle.gradleUserHomeDir
    )
}

internal open class KotlinBuildStatsLoggerService(
    configuration: KotlinBuildStatsConfiguration,
) {
    companion object {
        private val logger = Logging.getLogger(KotlinBuildStatsLoggerService::class.java)
    }

    internal val sessionLogger = BuildSessionLogger(
        configuration.sessionLoggerRootPath,
        forceValuesValidation = configuration.forcePropertiesValidation,
    )

    fun initSessionLogger(buildId: String) {
        runMetricMethodSafely(logger, "${KotlinBuildStatsLoggerService::class.java}.initSessionLogger") {
            if (!sessionLogger.isBuildSessionStarted()) {
                sessionLogger.startBuildSession(buildId)
            } else {
                logger.debug("Unable to initiate build logger for \"$buildId\" build. It is already initialized for \"${sessionLogger.getActiveBuildId()}\" build")
            }
        }
    }

    internal fun reportBuildFinished(metricsContainer: NonSynchronizedMetricsContainer? = null) {
        runMetricMethodSafely(logger, "${KotlinBuildStatsLoggerService::class.java}.reportBuildFinish") {
            metricsContainer?.sendToConsumer(sessionLogger)
            sessionLogger.finishBuildSession()
        }
    }
}
