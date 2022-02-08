/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.initialization.BuildRequestMetaData
import org.gradle.invocation.DefaultGradle
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatHandler.Companion.runSafe
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.statistics.BuildSessionLogger.Companion.STATISTICS_FOLDER_NAME
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.IStatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.StandardMBean
import kotlin.system.measureTimeMillis

/**
 * Interface for populating statistics collection method via JXM interface
 * JMX could be used for reporting both from other JVMs, other versions
 * of Kotlin Plugin and other classloaders
 */
interface KotlinBuildStatsMXBean {
    fun reportBoolean(name: String, value: Boolean, subprojectName: String?)

    fun reportNumber(name: String, value: Long, subprojectName: String?)

    fun reportString(name: String, value: String, subprojectName: String?)
}


internal abstract class KotlinBuildStatsService internal constructor() : BuildAdapter(), IStatisticsValuesConsumer {
    companion object {
        // Do not rename this bean otherwise compatibility with the older Kotlin Gradle Plugins would be lost
        const val JMX_BEAN_NAME = "org.jetbrains.kotlin.gradle.plugin.statistics:type=StatsService"

        // Property name for disabling saving statistical information
        const val ENABLE_STATISTICS_PROPERTY_NAME = "enable_kotlin_performance_profile"

        // default state
        const val DEFAULT_STATISTICS_STATE = true

        // "emergency file" collecting statistics is disabled it the file exists
        const val DISABLE_STATISTICS_FILE_NAME = "${STATISTICS_FOLDER_NAME}/.disable"

        /**
         * Method for getting IStatisticsValuesConsumer for reporting some statistics
         * Could be invoked during any build phase after applying first Kotlin plugin and
         * until build is completed
         */
        @JvmStatic
        @Synchronized
        fun getInstance(): IStatisticsValuesConsumer? {
            if (statisticsIsEnabled != true) {
                return null
            }
            return instance
        }

        /**
         * Method for creating new instance of IStatisticsValuesConsumer
         * It could be invoked only when applying Kotlin gradle plugin.
         * When executed, this method checks, whether it is already executed in current build.
         * If it was not executed, the new instance of IStatisticsValuesConsumer is created
         *
         * If it was already executed in the same classpath (i.e. with the same version of Kotlin plugin),
         * the previously returned instance is returned.
         *
         * If it was already executed in the other classpath, a JXM implementation is returned.
         *
         * All the created instances are registered as build listeners
         */
        @JvmStatic
        @Synchronized
        internal fun getOrCreateInstance(project: Project): IStatisticsValuesConsumer? {

            return runSafe("${KotlinBuildStatsService::class.java}.getOrCreateInstance") {
                val gradle = project.gradle
                statisticsIsEnabled = statisticsIsEnabled ?: checkStatisticsEnabled(gradle)
                if (statisticsIsEnabled != true) {
                    null
                } else {
                    val log = getLogger()

                    if (instance != null) {
                        log.debug("${KotlinBuildStatsService::class.java} is already instantiated. Current instance is $instance")
                    } else {
                        val beanName = ObjectName(JMX_BEAN_NAME)
                        val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
                        if (mbs.isRegistered(beanName)) {
                            log.debug(
                                "${KotlinBuildStatsService::class.java} is already instantiated in another classpath. Creating JMX-wrapper"
                            )
                            instance = JMXKotlinBuildStatsService(mbs, beanName)
                        } else {
                            val newInstance = DefaultKotlinBuildStatsService(gradle, beanName)

                            instance = newInstance
                            log.debug("Instantiated ${KotlinBuildStatsService::class.java}: new instance $instance")
                            mbs.registerMBean(StandardMBean(newInstance, KotlinBuildStatsMXBean::class.java), beanName)
                        }

                        if (!isConfigurationCacheAvailable(gradle)) {
                            gradle.addBuildListener(instance!!)
                        }
                    }
                    instance
                }
            }
        }

        /**
         * Invokes provided collector if the reporting service is initialised.
         * The duration of collector's wall time is reported into overall overhead metric.
         */
        fun applyIfInitialised(collector: (IStatisticsValuesConsumer) -> Unit) {
            getInstance()?.apply {
                try {
                    val duration = measureTimeMillis {
                        collector.invoke(this)
                    }
                    this.report(NumericalMetrics.STATISTICS_COLLECT_METRICS_OVERHEAD, duration)
                } catch (e: Throwable) {
                    KotlinBuildStatHandler.logException("Could collect statistics metrics", e)
                }
            }
        }

        @JvmStatic
        internal fun getLogger() = Logging.getLogger(KotlinBuildStatsService::class.java)

        internal var instance: KotlinBuildStatsService? = null

        private var statisticsIsEnabled: Boolean? = null

        private fun checkStatisticsEnabled(gradle: Gradle): Boolean {
            return if (File(gradle.gradleUserHomeDir, DISABLE_STATISTICS_FILE_NAME).exists()) {
                false
            } else {
                gradle.rootProject.properties[ENABLE_STATISTICS_PROPERTY_NAME]?.toString()?.toBoolean() ?: DEFAULT_STATISTICS_STATE
            }
        }
    }
}

internal class JMXKotlinBuildStatsService(private val mbs: MBeanServer, private val beanName: ObjectName) :
    KotlinBuildStatsService() {

    private fun callJmx(method: String, type: String, metricName: String, value: Any, subprojectName: String?) {
        mbs.invoke(
            beanName,
            method,
            arrayOf(metricName, value, subprojectName),
            arrayOf("java.lang.String", type, "java.lang.String")
        )
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            callJmx("reportBoolean", "boolean", metric.name, value, subprojectName)
        }
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            callJmx("reportNumber", "long", metric.name, value, subprojectName)
        }
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            callJmx("reportString", "java.lang.String", metric.name, value, subprojectName)
        }
    }

    override fun buildFinished(result: BuildResult) {
        instance = null
    }
}

internal class DefaultKotlinBuildStatsService internal constructor(
    gradle: Gradle,
    val beanName: ObjectName
) : KotlinBuildStatsService(), KotlinBuildStatsMXBean {
    private val sessionLogger = BuildSessionLogger(gradle.gradleUserHomeDir)

    private fun gradleBuildStartTime(gradle: Gradle): Long? {
        return (gradle as? DefaultGradle)?.services?.get(BuildRequestMetaData::class.java)?.startTime
    }

    override fun projectsEvaluated(gradle: Gradle) {
        runSafe("${DefaultKotlinBuildStatsService::class.java}.projectEvaluated") {
            if (!sessionLogger.isBuildSessionStarted()) {
                sessionLogger.startBuildSession(
                    DaemonReuseCounter.incrementAndGetOrdinal(),
                    gradleBuildStartTime(gradle)
                )
            }
        }
    }

    @Synchronized
    override fun buildFinished(result: BuildResult) {
        KotlinBuildStatHandler().buildFinished(result.gradle, beanName, sessionLogger, result.action, result.failure)
        instance = null
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?) {
        KotlinBuildStatHandler().report(sessionLogger, metric, value, subprojectName)
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?) {
        KotlinBuildStatHandler().report(sessionLogger, metric, value, subprojectName)
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?) {
        KotlinBuildStatHandler().report(sessionLogger, metric, value, subprojectName)
    }

    override fun reportBoolean(name: String, value: Boolean, subprojectName: String?) {
        report(BooleanMetrics.valueOf(name), value, subprojectName)
    }

    override fun reportNumber(name: String, value: Long, subprojectName: String?) {
        report(NumericalMetrics.valueOf(name), value, subprojectName)
    }

    override fun reportString(name: String, value: String, subprojectName: String?) {
        report(StringMetrics.valueOf(name), value, subprojectName)
    }
}
