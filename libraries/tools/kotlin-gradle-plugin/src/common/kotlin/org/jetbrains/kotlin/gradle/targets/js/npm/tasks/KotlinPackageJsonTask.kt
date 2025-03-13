/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PackageJsonProducerInputs
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.currentBuild
import org.jetbrains.kotlin.gradle.utils.mapToFile
import java.io.File

@DisableCachingByDefault
abstract class KotlinPackageJsonTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager,
    UsesGradleNodeModulesCache {
    // Only in configuration phase
    // Not part of configuration caching

    private val nodeJs: NodeJsRootExtension
        get() = project.rootProject.kotlinNodeJsRootExtension

    private val rootResolver: KotlinRootNpmResolver
        get() = nodeJs.resolver

    private val compilationResolver: KotlinCompilationNpmResolver
        get() = rootResolver[projectPath][compilationDisambiguatedName.get()]

    // -----

    private val projectPath = project.path

    @get:Internal
    abstract val compilationDisambiguatedName: Property<String>

    @get:Internal
    abstract val packageJsonHandlers: ListProperty<Action<PackageJson>>

    @get:Input
    abstract val packageJsonMain: Property<String>

    @get:Input
    internal val packageJsonInputHandlers: Provider<PackageJson> by lazy {
        packageJsonHandlers.map { packageJsonHandlersList ->
            PackageJson(fakePackageJsonValue, fakePackageJsonValue)
                .apply {
                    packageJsonHandlersList.forEach { it.execute(this) }
                }
        }
    }

    @get:Input
    internal val toolsNpmDependencies: List<String> by lazy {
        nodeJs.taskRequirements
            .getCompilationNpmRequirements(projectPath, compilationDisambiguatedName.get())
            .map { it.toString() }
            .sorted()
    }

    /**
     * Contains `package.json` files from Kotlin/JS projects (not external dependencies) that the current project depends on.
     *
     * Required for up-to-date checks:
     * If the npm dependencies of any dependency change, this task should re-run.
     *
     * This should only contain files from composite build dependencies.
     * Other dependencies are handled specially - see
     * [org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver].
     */
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val packageJsonFilesFromCompositeBuildDependencies: ConfigurableFileCollection

    // nested inputs are processed in configuration phase
    // so npmResolutionManager must not be used
    @get:Nested
    internal val producerInputs: PackageJsonProducerInputs by lazy {
        compilationResolver.compilationNpmResolution.inputs
    }

    @get:OutputFile
    abstract val packageJson: Property<File>

    @TaskAction
    fun resolve() {
        val resolution = npmResolutionManager.get().resolution.get()[projectPath][compilationDisambiguatedName.get()]
        val preparedResolution = resolution
            .prepareWithDependencies(
                npmResolutionManager = npmResolutionManager.get(),
                logger = logger
            )

        resolution.createPackageJson(preparedResolution, packageJsonMain, packageJsonHandlers)
    }

    companion object {
        fun create(compilation: KotlinJsIrCompilation): TaskProvider<KotlinPackageJsonTask> {
            val target = compilation.target
            val project = target.project
            val npmProject = compilation.npmProject
            val nodeJsTaskProviders = project.rootProject.kotlinNodeJsRootExtension

            val npmCachesSetupTask = nodeJsTaskProviders.npmCachesSetupTaskProvider
            val packageJsonTaskName = npmProject.packageJsonTaskName
            val packageJsonUmbrella = nodeJsTaskProviders.packageJsonUmbrellaTaskProvider
            val npmResolutionManager = project.kotlinNpmResolutionManager
            val nodeJsRoot: NodeJsRootExtension = npmProject.nodeJsRoot
            val gradleNodeModules = GradleNodeModulesCache.registerIfAbsent(project, null, null)
            val packageJsonTask = project.registerTask<KotlinPackageJsonTask>(packageJsonTaskName) { task ->
                task.compilationDisambiguatedName.set(compilation.disambiguatedName)
                task.packageJsonHandlers.set(compilation.packageJsonHandlers)
                task.description = "Create package.json file for $compilation"
                task.group = NodeJsRootPlugin.TASKS_GROUP_NAME

                task.npmResolutionManager.value(npmResolutionManager)
                    .disallowChanges()

                task.gradleNodeModules.value(gradleNodeModules)
                    .disallowChanges()

                task.packageJsonMain.set(compilation.npmProject.main)

                task.packageJson.set(compilation.npmProject.packageJsonFile.mapToFile())

                val compilationDisambiguatedName = compilation.disambiguatedName

                configurePackageJsonFilesFromProjectDependencies(
                    task = task,
                    project = project,
                    nodeJsRoot = nodeJsRoot,
                    compilationDisambiguatedName = compilationDisambiguatedName,
                )

                task.onlyIf {
                    it as KotlinPackageJsonTask
                    it.npmResolutionManager.get().isConfiguringState()
                }

                task.dependsOn(npmCachesSetupTask)
            }

            packageJsonUmbrella.configure { task ->
                task.inputs.file(packageJsonTask.map { it.packageJson })
            }

            nodeJsTaskProviders.rootPackageJsonTaskProvider.configure { it.mustRunAfter(packageJsonTask) }

            return packageJsonTask
        }

        private fun configurePackageJsonFilesFromProjectDependencies(
            task: KotlinPackageJsonTask,
            project: Project,
            nodeJsRoot: NodeJsRootExtension,
            compilationDisambiguatedName: String,
        ) {
            val projectPath = project.path
            fun getCompilationResolver(): KotlinCompilationNpmResolver =
                getCompilationResolver(
                    nodeJsRoot,
                    projectPath,
                    compilationDisambiguatedName
                )

            val aggregatedConfiguration = project.provider {
                getCompilationResolver().aggregatedConfiguration
            }

            val currentBuild = project.currentBuild
            fun ComponentIdentifier.isFromCompositeBuild(): Boolean =
                this is ProjectComponentIdentifier && this !in currentBuild

            task.packageJsonFilesFromCompositeBuildDependencies
                .from(
                    aggregatedConfiguration.map { conf ->
                        conf.incoming
                            .artifactView { artifactView ->
                                artifactView.componentFilter { componentIdentifier ->
                                    componentIdentifier.isFromCompositeBuild()
                                }
                            }
                            .artifacts
                            .artifactFiles
                            // Convert from a Gradle type to a regular collection to remove implicit task dependencies.
                            // The dependent tasks are added back manually below using findDependantTasks().
                            .toSet()
                    }
                )
                .disallowChanges()

            // Manually declare the required tasks, otherwise unnecessary compilation tasks will be triggered.
            // (Because DefaultKotlinCompilationAssociator automatically adds main files to auxiliary (test) compilations,
            // and these files will automatically trigger compilation).
            task.dependsOn(
                project.provider {
                    findDependentTasks(
                        rootResolver = nodeJsRoot.resolver,
                        compilationNpmResolution = getCompilationResolver().compilationNpmResolution,
                    )
                }
            )
        }

        private fun findDependentTasks(
            rootResolver: KotlinRootNpmResolver,
            compilationNpmResolution: KotlinCompilationNpmResolution,
        ): Collection<Any> {
            val internalTasks = compilationNpmResolution.internalDependencies.map { dependency ->
                rootResolver[dependency.projectPath][dependency.compilationName].npmProject.packageJsonTaskPath
            }

            val compositeBuildTasks = compilationNpmResolution.internalCompositeDependencies.map { dependency ->
                dependency.includedBuild ?: error("includedBuild instance is not available")
                dependency.includedBuild.task(":${RootPackageJsonTask.NAME}")
            }

            return internalTasks + compositeBuildTasks
        }

        private fun getCompilationResolver(
            nodeJsRoot: NodeJsRootExtension,
            projectPath: String,
            compilationDisambiguatedName: String,
        ): KotlinCompilationNpmResolver =
            nodeJsRoot.resolver[projectPath][compilationDisambiguatedName]
    }
}
