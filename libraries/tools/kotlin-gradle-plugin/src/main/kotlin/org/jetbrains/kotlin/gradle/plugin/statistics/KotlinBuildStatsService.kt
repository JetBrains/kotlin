/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.initialization.BuildCompletionListener
import org.gradle.initialization.BuildRequestMetaData
import org.gradle.invocation.DefaultGradle
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.statistics.BuildSessionLogger.Companion.STATISTICS_FOLDER_NAME
import org.jetbrains.kotlin.statistics.metrics.*
import java.io.File
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.StandardMBean

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


internal abstract class KotlinBuildStatsService internal constructor() : BuildAdapter(), IStatisticsValuesConsumer,
    BuildCompletionListener {

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
        internal fun getOrCreateInstance(gradle: Gradle): IStatisticsValuesConsumer? {
            return runSafe("${KotlinBuildStatsService::class.java}.getOrCreateInstance") {
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
                    }
                    gradle.addBuildListener(instance)
                    instance
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

        private fun logException(description: String, e: Throwable) {
            getLogger().info(description)
            getLogger().debug(e.message, e)
        }

        internal fun <T> runSafe(methodName: String, action: () -> T?): T? {
            return try {
                getLogger().debug("Executing [$methodName]")
                action.invoke()
            } catch (e: Throwable) {
                logException("Could not execute [$methodName]", e)
                null
            }
        }
    }


}

internal class JMXKotlinBuildStatsService(private val mbs: MBeanServer, private val beanName: ObjectName) :
    KotlinBuildStatsService() {

    override fun buildFinished(result: BuildResult) {
    }

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

    override fun completed() {
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

    private fun reportGlobalMetrics(gradle: Gradle) {
        System.getProperty("os.name")?.also {
            sessionLogger.report(StringMetrics.OS_TYPE, gradle.gradleVersion)
        }
        sessionLogger.report(NumericalMetrics.CPU_NUMBER_OF_CORES, Runtime.getRuntime().availableProcessors().toLong())
        sessionLogger.report(StringMetrics.GRADLE_VERSION, gradle.gradleVersion)
        sessionLogger.report(BooleanMetrics.EXECUTED_FROM_IDEA, System.getProperty("idea.active") != null)


        gradle.allprojects { project ->
            for (configuration in project.configurations) {
                val configurationName = configuration.name
                val dependencies = configuration.dependencies

                when (configurationName) {
                    "kapt" -> {
                        sessionLogger.report(BooleanMetrics.ENABLED_KAPT, true)
                        dependencies.forEach { dependency ->
                            when (dependency.group) {
                                "com.google.dagger" -> sessionLogger.report(BooleanMetrics.ENABLED_DAGGER, true)
                                "com.android.databinding" -> sessionLogger.report(BooleanMetrics.ENABLED_DATABINDING, true)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun projectsEvaluated(gradle: Gradle) {
        runSafe("${DefaultKotlinBuildStatsService::class.java}.projectEvaluated") {
            if (!sessionLogger.isBuildSessionStarted()) {
                sessionLogger.startBuildSession(
                    DaemonReuseCounter.incrementAndGetOrdinal(),
                    gradleBuildStartTime(gradle)
                )
                reportGlobalMetrics(gradle)
            }
        }
    }

    @Synchronized
    override fun buildFinished(result: BuildResult) {
        runSafe("${DefaultKotlinBuildStatsService::class.java}.buildFinished") {
            sessionLogger.finishBuildSession(result.action, result.failure)
        }
    }


    @Synchronized
    override fun completed() {
        runSafe("${DefaultKotlinBuildStatsService::class.java}.completed") {
            try {
                sessionLogger.unlockJournalFile()
            } finally {
                val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
                if (mbs.isRegistered(beanName)) {
                    mbs.unregisterMBean(beanName)
                }
                instance = null
            }
        }
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            sessionLogger.report(metric, value, subprojectName)
        }

    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            sessionLogger.report(metric, value, subprojectName)
        }
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            sessionLogger.report(metric, value, subprojectName)
        }
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
