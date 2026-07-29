/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.PluginLoadedInMultipleProjectsError
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.kotlinSessionsDir
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.io.File

internal abstract class KotlinGradleBuildServices : BuildService<KotlinGradleBuildServices.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        val sessionsDir: Property<File>
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
                    val propertiesProvider = PropertiesProvider(project)
                    if (propertiesProvider.ignorePluginLoadedInMultipleProjects != true) {
                        project.reportDiagnostic(PluginLoadedInMultipleProjectsError(loadedInProjects))
                    }
                    project.logger.info(
                        MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_INFO + loadedInProjects.joinToString { "'$it'" }
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
        buildHandler.buildFinished(parameters.sessionsDir.get())
        log.kotlinDebug(DISPOSE_MESSAGE)
    }

    companion object {
        private val CLASS_NAME = KotlinGradleBuildServices::class.java.simpleName
        private val INIT_MESSAGE = "Initialized $CLASS_NAME"
        private val DISPOSE_MESSAGE = "Disposed $CLASS_NAME"

        /**
         * Registers the build service if absent and starts it to ensure that [KotlinGradleFinishBuildHandler]'s
         * "build start" callback is invoked. Call this method early in the plugin lifecycle, when the plugin is applied.
         */
        fun registerIfAbsentAndStart(project: Project) {
            project.gradle.registerClassLoaderScopedBuildService(KotlinGradleBuildServices::class) {
                it.parameters.sessionsDir.set(project.kotlinSessionsDir)
            }.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesKotlinGradleBuildServices::class.java.name) {
                    project.tasks.withType<UsesKotlinGradleBuildServices>().configureEach { task ->
                        task.usesService(serviceProvider)
                        task.kotlinGradleBuildServices.set(serviceProvider)
                    }
                }
            }.get()
        }
    }
}

internal interface UsesKotlinGradleBuildServices : Task {
    @get:Internal
    val kotlinGradleBuildServices: Property<KotlinGradleBuildServices>
}


