/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.initialization.BuildRequestMetaData
import org.gradle.invocation.DefaultGradle
import org.jetbrains.kotlin.gradle.utils.API
import org.jetbrains.kotlin.gradle.utils.COMPILE
import org.jetbrains.kotlin.gradle.utils.IMPLEMENTATION
import org.jetbrains.kotlin.gradle.utils.RUNTIME
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
                    logException("Could collect statistics metrics", e)
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

    private fun reportLibrariesVersions(dependencies: DependencySet?) {
        dependencies?.forEach { dependency ->
            when {
                dependency.group?.startsWith("org.springframework") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_SPRING_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("com.vaadin") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_VAADIN_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("com.google.gwt") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_GWT_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("org.hibernate") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_HIBERNATE_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlin" && dependency.name.startsWith("kotlin-stdlib") -> sessionLogger.report(
                    StringMetrics.KOTLIN_STDLIB_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlinx" && dependency.name == "kotlinx-coroutines" -> sessionLogger.report(
                    StringMetrics.KOTLIN_COROUTINES_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlin" && dependency.name == "kotlin-reflect" -> sessionLogger.report(
                    StringMetrics.KOTLIN_REFLECT_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlinx" && dependency.name
                    .startsWith("kotlinx-serialization-runtime") -> sessionLogger.report(
                    StringMetrics.KOTLIN_SERIALIZATION_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "com.android.tools.build" && dependency.name.startsWith("gradle") -> sessionLogger.report(
                    StringMetrics.ANDROID_GRADLE_PLUGIN_VERSION,
                    dependency.version ?: "0.0.0"
                )
            }
        }
    }

    private fun reportGlobalMetrics(gradle: Gradle) {
        System.getProperty("os.name")?.also {
            sessionLogger.report(StringMetrics.OS_TYPE, System.getProperty("os.name"))
        }
        sessionLogger.report(NumericalMetrics.CPU_NUMBER_OF_CORES, Runtime.getRuntime().availableProcessors().toLong())
        sessionLogger.report(StringMetrics.GRADLE_VERSION, gradle.gradleVersion)
        sessionLogger.report(BooleanMetrics.EXECUTED_FROM_IDEA, System.getProperty("idea.active") != null)
        sessionLogger.report(NumericalMetrics.GRADLE_DAEMON_HEAP_SIZE, Runtime.getRuntime().maxMemory())
        sessionLogger.report(
            BooleanMetrics.KOTLIN_OFFICIAL_CODESTYLE,
            gradle.rootProject.properties["kotlin.code.style"] == "official"
        ) // constants are saved in IDEA plugin and could not be accessed directly

        gradle.taskGraph.whenReady() { taskExecutionGraph ->
            val executedTaskNames = taskExecutionGraph.allTasks.map { it.name }.distinct()
            report(BooleanMetrics.COMPILATION_STARTED, executedTaskNames.contains("compileKotlin"))
            report(BooleanMetrics.TESTS_EXECUTED, executedTaskNames.contains("compileTestKotlin"))
            report(BooleanMetrics.MAVEN_PUBLISH_EXECUTED, executedTaskNames.contains("install"))
        }

        fun buildSrcExists(project: Project) = File(project.projectDir, "buildSrc").exists()
        if (buildSrcExists(gradle.rootProject)) {
            sessionLogger.report(BooleanMetrics.BUILD_SRC_EXISTS, true)
        }
        val statisticOverhead = measureTimeMillis {
            gradle.allprojects { project ->
                for (configuration in project.configurations) {
                    val configurationName = configuration.name
                    val dependencies = configuration.dependencies

                    when (configurationName) {
                        "kapt" -> {
                            sessionLogger.report(BooleanMetrics.ENABLED_KAPT, true)
                            dependencies?.forEach { dependency ->
                                when (dependency.group) {
                                    "com.google.dagger" -> sessionLogger.report(BooleanMetrics.ENABLED_DAGGER, true)
                                    "com.android.databinding" -> sessionLogger.report(BooleanMetrics.ENABLED_DATABINDING, true)
                                }
                            }
                        }
                        API -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_API_COUNT, 1)
                            reportLibrariesVersions(dependencies)
                        }
                        IMPLEMENTATION -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_IMPLEMENTATION_COUNT, 1)
                            reportLibrariesVersions(dependencies)
                        }
                        COMPILE -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_COMPILE_COUNT, 1)
                            reportLibrariesVersions(dependencies)
                        }
                        RUNTIME -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_RUNTIME_COUNT, 1)
                            reportLibrariesVersions(dependencies)
                        }
                    }
                }

                sessionLogger.report(NumericalMetrics.NUMBER_OF_SUBPROJECTS, 1)
                sessionLogger.report(BooleanMetrics.KOTLIN_KTS_USED, project.buildscript.sourceFile?.name?.endsWith(".kts") ?: false)
                sessionLogger.report(NumericalMetrics.GRADLE_NUMBER_OF_TASKS, project.tasks.names.size.toLong())
                sessionLogger.report(
                    NumericalMetrics.GRADLE_NUMBER_OF_UNCONFIGURED_TASKS,
                    project.tasks.names.count { name ->
                        try {
                            project.tasks.named(name).javaClass.name.contains("TaskCreatingProvider")
                        } catch (_: Exception) {
                            true
                        }
                    }.toLong()
                )

                if (buildSrcExists(project)) {
                    sessionLogger.report(NumericalMetrics.BUILD_SRC_COUNT, 1)
                    sessionLogger.report(BooleanMetrics.BUILD_SRC_EXISTS, true)
                }
            }
        }
        sessionLogger.report(NumericalMetrics.STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD, statisticOverhead)
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
        runSafe("${DefaultKotlinBuildStatsService::class.java}.buildFinished") {
            try {
                try {
                    val gradle = result.gradle
                    if (gradle != null) reportGlobalMetrics(gradle)
                } finally {
                    sessionLogger.finishBuildSession(result.action, result.failure)
                }
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
