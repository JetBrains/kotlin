/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.compilerRunner.DELETED_SESSION_FILE_PREFIX
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers
import org.jetbrains.kotlin.gradle.report.configureBuildReporter
import org.jetbrains.kotlin.gradle.utils.relativeToRoot
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import java.lang.management.ManagementFactory
import kotlin.math.max

internal class KotlinGradleBuildServices private constructor(
    private val gradle: Gradle
) : BuildAdapter() {
    companion object {
        private val CLASS_NAME = KotlinGradleBuildServices::class.java.simpleName
        const val FORCE_SYSTEM_GC_MESSAGE = "Forcing System.gc()"
        const val SHOULD_REPORT_MEMORY_USAGE_PROPERTY = "kotlin.gradle.test.report.memory.usage"

        val INIT_MESSAGE = "Initialized $CLASS_NAME"
        val DISPOSE_MESSAGE = "Disposed $CLASS_NAME"
        val ALREADY_INITIALIZED_MESSAGE = "$CLASS_NAME is already initialized"

        @field:Volatile
        private var instance: KotlinGradleBuildServices? = null

        @JvmStatic
        @Synchronized
        fun getInstance(gradle: Gradle): KotlinGradleBuildServices {
            val log = Logging.getLogger(KotlinGradleBuildServices::class.java)

            if (instance != null) {
                log.kotlinDebug(ALREADY_INITIALIZED_MESSAGE)
                return instance!!
            }

            val services = KotlinGradleBuildServices(gradle)
            gradle.addBuildListener(services)
            instance = services
            log.kotlinDebug(INIT_MESSAGE)

            services.buildStarted()
            return services
        }
    }

    private val log = Logging.getLogger(this.javaClass)
    private var startMemory: Long? = null
    private val shouldReportMemoryUsage = System.getProperty(SHOULD_REPORT_MEMORY_USAGE_PROPERTY) != null

    // There is function with the same name in BuildAdapter,
    // but it is called before any plugin can attach build listener
    fun buildStarted() {
        startMemory = getUsedMemoryKb()

        TaskLoggers.clear()
        TaskExecutionResults.clear()

        configureBuildReporter(gradle, log)
    }

    override fun buildFinished(result: BuildResult) {
        TaskLoggers.clear()
        TaskExecutionResults.clear()

        val gradle = result.gradle!!
        GradleCompilerRunner.clearBuildModulesInfo()

        val rootProject = gradle.rootProject
        val sessionsDir = GradleCompilerRunner.sessionsDir(rootProject)
        if (sessionsDir.exists()) {
            val sessionFiles = sessionsDir.listFiles()

            // it is expected that only one session file per build exists
            // afaik is is not possible to run multiple gradle builds in one project since gradle locks some dirs
            if (sessionFiles.size > 1) {
                log.warn("w: Detected multiple Kotlin daemon sessions at ${sessionsDir.relativeToRoot(rootProject)}")
            }
            for (file in sessionFiles) {
                file.delete()
                log.kotlinDebug { DELETED_SESSION_FILE_PREFIX + file.relativeToRoot(rootProject) }
            }
        }

        if (shouldReportMemoryUsage) {
            val startMem = startMemory!!
            val endMem = getUsedMemoryKb()!!

            // the value reported here is not necessarily a leak, since it is calculated before collecting the plugin classes
            // but on subsequent runs in the daemon it should be rather small, then the classes are actually reused by the daemon (see above)
            log.lifecycle("[KOTLIN][PERF] Used memory after build: $endMem kb (difference since build start: ${"%+d".format(endMem - startMem)} kb)")
        }

        gradle.removeListener(this)
        instance = null
        log.kotlinDebug(DISPOSE_MESSAGE)
    }

    private fun getUsedMemoryKb(): Long? {
        if (!shouldReportMemoryUsage) return null

        log.lifecycle(FORCE_SYSTEM_GC_MESSAGE)
        val gcCountBefore = getGcCount()
        System.gc()
        while (getGcCount() == gcCountBefore) {
        }

        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / 1024
    }

    private fun getGcCount(): Long =
        ManagementFactory.getGarbageCollectorMXBeans().sumByLong { max(0, it.collectionCount) }

    private val multipleProjectsHolder = KotlinPluginInMultipleProjectsHolder(
        differentVersionsInDifferentProject = true
    )

    @Synchronized
    internal fun detectKotlinPluginLoadedInMultipleProjects(project: Project, kotlinPluginVersion: String) {
        val onRegister = {
            gradle.taskGraph.whenReady {
                if (multipleProjectsHolder.isInMultipleProjects(project, kotlinPluginVersion)) {
                    val loadedInProjects = multipleProjectsHolder.getAffectedProjects(project, kotlinPluginVersion)!!
                    if (PropertiesProvider(project).ignorePluginLoadedInMultipleProjects != true) {
                        project.logger.warn(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)
                        project.logger.warn(
                            MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING + loadedInProjects.joinToString(limit = 4) { "'$it'" }
                        )
                    }
                    project.logger.info(
                        "$MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_INFO: " +
                                loadedInProjects.joinToString { "'$it'" }
                    )
                }
            }
        }

        multipleProjectsHolder.addProject(
            project,
            kotlinPluginVersion,
            onRegister
        )
    }
}


