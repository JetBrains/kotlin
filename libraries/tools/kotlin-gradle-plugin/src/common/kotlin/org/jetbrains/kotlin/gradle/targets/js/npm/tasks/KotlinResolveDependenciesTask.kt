/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.tasks.registerTask

@DisableCachingByDefault
abstract class KotlinResolveDependenciesTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager,
    UsesGradleNodeModulesCache {
    // Only in configuration phase
    // Not part of configuration caching

    private val nodeJs: NodeJsRootExtension
        get() = project.rootProject.kotlinNodeJsExtension

    private val rootResolver: KotlinRootNpmResolver
        get() = nodeJs.resolver

    // -----

    private val projectPath = project.path

    @get:Internal
    abstract val compilationDisambiguatedName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val dependencyConfiguration: ConfigurableFileCollection = project.objects
        .fileCollection()

    @get:Internal
    internal val components by lazy {
        rootResolver.allResolvedConfigurations
    }

    @TaskAction
    fun resolve() {
        val resolution = npmResolutionManager.get().resolution.get()[projectPath][compilationDisambiguatedName.get()]
        resolution
            .prepareWithDependencies(
                npmResolutionManager = npmResolutionManager.get(),
                logger = logger,
                resolvedConfigurations = components
            )
    }

    companion object {
        fun create(
            compilation: KotlinJsIrCompilation,
            dependencyConfiguration: Configuration,
            disambiguatedName: String,
        ): TaskProvider<KotlinResolveDependenciesTask> {
            val project = compilation.target.project
            val npmProject = compilation.npmProject
            val nodeJsTaskProviders = project.rootProject.kotlinNodeJsExtension

            val npmCachesSetupTask = nodeJsTaskProviders.npmCachesSetupTaskProvider
            val resolveDependenciesTaskName = npmProject.resolveDependenciesTaskName

            val npmResolutionManager = project.kotlinNpmResolutionManager
            val gradleNodeModules = GradleNodeModulesCache.registerIfAbsent(project, null, null)
            val packageJsonTask = project.registerTask<KotlinResolveDependenciesTask>(resolveDependenciesTaskName) { task ->
                task.compilationDisambiguatedName.set(disambiguatedName)
                task.description = "Resolve dependencies for further package.json gneration in $compilation"
                task.group = NodeJsRootPlugin.TASKS_GROUP_NAME

                task.dependencyConfiguration.from(dependencyConfiguration)

                task.npmResolutionManager.value(npmResolutionManager)
                    .disallowChanges()

                task.gradleNodeModules.value(gradleNodeModules)
                    .disallowChanges()

                task.onlyIf {
                    it as KotlinResolveDependenciesTask
                    it.npmResolutionManager.get().isConfiguringState()
                }

                task.dependsOn(npmCachesSetupTask)
            }

            return packageJsonTask
        }
    }
}