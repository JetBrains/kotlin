/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.internal.ConfigurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.utils.runMetricMethodSafely
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.statistics.BuildSessionLogger.Companion.STATISTICS_FOLDER_NAME
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.Closeable
import java.io.File
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName

/**
 * Interface for populating statistics collection method via JXM interface
 * JMX could be used for reporting both from other JVMs, other versions
 * of Kotlin Plugin and other classloaders
 */

interface KotlinBuildStatsMXBean {
    fun reportBoolean(name: String, value: Boolean, subprojectName: String?, weight: Long?): Boolean

    fun reportNumber(name: String, value: Long, subprojectName: String?, weight: Long?): Boolean

    fun reportString(name: String, value: String, subprojectName: String?, weight: Long?): Boolean
}

internal abstract class KotlinBuildStatsBeanService internal constructor(
    project: Project,
    private val beanName: ObjectName,
) : StatisticsValuesConsumer, Closeable {

    companion object {
        // Property name for disabling saving statistical information
        private const val ENABLE_STATISTICS_PROPERTY_NAME = "enable_kotlin_performance_profile"

        // default state
        private const val DEFAULT_STATISTICS_STATE = true

        // "emergency file" collecting statistics is disabled it the file exists
        private const val DISABLE_STATISTICS_FILE_NAME = "${STATISTICS_FOLDER_NAME}/.disable"

        @JvmStatic
        internal val logger = Logging.getLogger(KotlinBuildStatsBeanService::class.java)

        /**
         * Method for creating new instance of [StatisticsValuesConsumer]
         * It could be invoked only when applying Kotlin gradle plugin.
         * When executed, this method checks, whether it is already executed in the current build (whether it was already executed
         * in the same classpath (i.e., with the same version of Kotlin plugin)).
         * If it was not executed, the new instance of StatisticsValuesConsumer is created
         *
         * [closeServices] must be called at the end of the build in order to release resources.
         */
        @JvmStatic
        @Synchronized
        internal fun initStatsService(project: Project) {
            runMetricMethodSafely(logger, "${KotlinBuildStatsBeanService::class.java}.initStatsService") {
                val gradle = project.gradle
                val configurationTimePropertiesAccessor = project.configurationTimePropertiesAccessor
                val statisticsIsEnabled = checkStatisticsEnabled(gradle, project.providers, configurationTimePropertiesAccessor)
                if (!statisticsIsEnabled) {
                    null
                } else {
                    //register JMX service to support old kotlin version compatibility

                    val registry = kotlinBuildStatsServicesRegistry ?: KotlinBuildStatsServicesRegistry().also {
                        kotlinBuildStatsServicesRegistry = it
                    }

                    registry.registerServices(project)
                }
            }
        }

        @Synchronized
        internal fun recordBuildStart(buildId: String) {
            kotlinBuildStatsServicesRegistry?.recordBuildStart(buildId)
        }

        @Synchronized
        fun closeServices() {
            kotlinBuildStatsServicesRegistry?.close()
            kotlinBuildStatsServicesRegistry = null
        }

        private var kotlinBuildStatsServicesRegistry: KotlinBuildStatsServicesRegistry? = null

        private fun checkStatisticsEnabled(
            gradle: Gradle,
            providerFactory: ProviderFactory,
            configurationTimePropertiesAccessor: ConfigurationTimePropertiesAccessor,
        ): Boolean {
            return if (File(gradle.gradleUserHomeDir, DISABLE_STATISTICS_FILE_NAME).exists()) {
                false
            } else {
                providerFactory.gradleProperty(ENABLE_STATISTICS_PROPERTY_NAME)
                    .usedAtConfigurationTime(configurationTimePropertiesAccessor)
                    .orNull?.toBoolean() ?: DEFAULT_STATISTICS_STATE
            }
        }
    }

    protected val kotlinBuildLogger = KotlinBuildStatsLoggerService(KotlinBuildStatsConfiguration(project))

    @Synchronized
    override fun close() {
        runMetricMethodSafely(logger, "${KotlinBuildStatsBeanService::class.java}.buildFinished") {
            val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
            if (mbs.isRegistered(beanName)) {
                mbs.unregisterMBean(beanName)
                logger.debug("Unregistered $this")
            }
        }
    }

    @Synchronized
    fun recordBuildStart(buildId: String) {
        kotlinBuildLogger.initSessionLogger(buildId)
    }

    internal fun report(
        sessionLogger: BuildSessionLogger,
        metric: BooleanMetrics,
        value: Boolean,
        subprojectName: String?,
        weight: Long? = null,
    ) = runMetricMethodSafely(logger, "report metric ${metric.name}") {
        sessionLogger.report(metric, value, subprojectName, weight)
    } ?: false

    internal fun report(
        sessionLogger: BuildSessionLogger,
        metric: NumericalMetrics,
        value: Long,
        subprojectName: String?,
        weight: Long? = null,
    ) = runMetricMethodSafely(logger, "report metric ${metric.name}") {
        sessionLogger.report(metric, value, subprojectName, weight)
    } ?: false

    internal fun report(
        sessionLogger: BuildSessionLogger,
        metric: StringMetrics,
        value: String,
        subprojectName: String?,
        weight: Long? = null,
    ) = runMetricMethodSafely(logger, "report metric ${metric.name}") {
        sessionLogger.report(metric, value, subprojectName, weight)
    } ?: false
}

internal class DefaultKotlinBuildStatsBeanService internal constructor(
    project: Project,
    beanName: ObjectName,
) : KotlinBuildStatsBeanService(project, beanName),
    KotlinBuildStatsMXBean {

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?): Boolean =
        report(kotlinBuildLogger.sessionLogger, metric, value, subprojectName, weight)

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?): Boolean =
        report(kotlinBuildLogger.sessionLogger, metric, value, subprojectName, weight)

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?): Boolean =
        report(kotlinBuildLogger.sessionLogger, metric, value, subprojectName, weight)

    override fun reportBoolean(name: String, value: Boolean, subprojectName: String?, weight: Long?): Boolean =
        report(BooleanMetrics.valueOf(name), value, subprojectName, weight)

    override fun reportNumber(name: String, value: Long, subprojectName: String?, weight: Long?): Boolean =
        report(NumericalMetrics.valueOf(name), value, subprojectName, weight)

    override fun reportString(name: String, value: String, subprojectName: String?, weight: Long?): Boolean =
        report(StringMetrics.valueOf(name), value, subprojectName, weight)

}
