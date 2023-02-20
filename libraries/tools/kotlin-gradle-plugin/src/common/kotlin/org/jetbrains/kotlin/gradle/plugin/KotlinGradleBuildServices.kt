/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.utils.projectCacheDir
import java.io.File

internal abstract class KotlinGradleBuildServices : BuildService<KotlinGradleBuildServices.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        var projectCacheDir: File
    }

    private val log = Logging.getLogger(this.javaClass)
    private val buildHandler: KotlinGradleFinishBuildHandler = KotlinGradleFinishBuildHandler()

    private val multipleProjectsHolder = KotlinPluginInMultipleProjectsHolder(
        trackPluginVersionsSeparately = true
    )

    init {
        log.kotlinDebug(INIT_MESSAGE)
        buildHandler.buildStart()
    }

    @Synchronized
    internal fun detectKotlinPluginLoadedInMultipleProjects(project: Project, kotlinPluginVersion: String) {
        val onRegister = {
            project.gradle.taskGraph.whenReady {
                if (multipleProjectsHolder.isInMultipleProjects(project, kotlinPluginVersion)) {
                    val loadedInProjects = multipleProjectsHolder.getAffectedProjects(project, kotlinPluginVersion)!!
                    if (PropertiesProvider(project).ignorePluginLoadedInMultipleProjects != true) {
                        project.logger.warn("\n$MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING")
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

    override fun close() {
        buildHandler.buildFinished(parameters.projectCacheDir)
        log.kotlinDebug(DISPOSE_MESSAGE)
    }

    companion object {
        private val CLASS_NAME = KotlinGradleBuildServices::class.java.simpleName
        private val INIT_MESSAGE = "Initialized $CLASS_NAME"
        private val DISPOSE_MESSAGE = "Disposed $CLASS_NAME"

        fun registerIfAbsent(gradle: Gradle): Provider<KotlinGradleBuildServices> =
            gradle.sharedServices.registerIfAbsent(
                "kotlin-build-service-${KotlinGradleBuildServices::class.java.canonicalName}_${KotlinGradleBuildServices::class.java.classLoader.hashCode()}",
                KotlinGradleBuildServices::class.java
            ) { service ->
                service.parameters.projectCacheDir = gradle.projectCacheDir
            }

    }
}


