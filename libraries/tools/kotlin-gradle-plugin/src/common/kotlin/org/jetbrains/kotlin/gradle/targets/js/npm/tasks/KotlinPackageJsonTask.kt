/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.*
import org.jetbrains.kotlin.gradle.targets.js.targetVariant
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.CompositeProjectComponentArtifactMetadata
import org.jetbrains.kotlin.gradle.utils.`is`
import org.jetbrains.kotlin.gradle.utils.mapToFile
import java.io.File
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNpmResolutionManager as wasmKotlinNpmResolutionManager

@DisableCachingByDefault
abstract class KotlinPackageJsonTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager,
    UsesGradleNodeModulesCache {

    private val projectPath = project.path

    @get:Internal
    abstract val compilationDisambiguatedName: Property<String>

    @get:Internal
    abstract val packageJsonHandlers: ListProperty<Action<PackageJson>>

    @get:Input
    abstract val packageJsonMain: Property<String>

    @get:Input
    internal abstract val packageJsonInputHandlers: Property<PackageJson>

    @get:Input
    internal abstract val toolsNpmDependencies: ListProperty<String>

    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val compositeFiles: ConfigurableFileCollection

    @get:Nested
    internal abstract val producerInputs: Property<PackageJsonProducerInputs>

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
            val packageJsonTaskName = npmProject.packageJsonTaskName

            val npmResolutionManager = compilation.targetVariant(
                { project.kotlinNpmResolutionManager },
                { project.wasmKotlinNpmResolutionManager },
            )

            val nodeJsRoot = npmProject.nodeJsRoot

            val packageJsonTask = project.registerTask<KotlinPackageJsonTask>(packageJsonTaskName) { task ->
                task.compilationDisambiguatedName.set(compilation.disambiguatedName)
                task.packageJsonHandlers.set(compilation.packageJsonHandlers)
                task.description = "Create package.json file for $compilation"
                task.group = NodeJsRootPlugin.TASKS_GROUP_NAME

                task.npmResolutionManager.value(npmResolutionManager)
                    .disallowChanges()

                task.gradleNodeModules.value(npmResolutionManager.flatMap { it.parameters.gradleNodeModulesProvider })
                    .disallowChanges()

                task.packageJsonMain.set(compilation.npmProject.main)

                task.packageJson.set(compilation.npmProject.packageJsonFile.mapToFile())

                task.packageJsonInputHandlers.value(
                    task.packageJsonHandlers.map { packageJsonHandlersList ->
                        PackageJson(fakePackageJsonValue, fakePackageJsonValue)
                            .apply {
                                packageJsonHandlersList.forEach { it.execute(this) }
                            }
                    }
                ).disallowChanges()

                val projectPath = project.path
                val compilationDisambiguatedName = compilation.disambiguatedName

                task.producerInputs.value(
                    project.provider {
                        // nested inputs are processed in configuration phase
                        // so npmResolutionManager must not be used
                        getCompilationResolver(nodeJsRoot, projectPath, compilationDisambiguatedName)
                            .compilationNpmResolution.inputs
                    }
                ).disallowChanges()

                task.compositeFiles.from(
                    project.provider {
                        getCompilationResolver(
                            nodeJsRoot,
                            projectPath,
                            compilationDisambiguatedName
                        ).aggregatedConfiguration
                            .incoming
                            .artifactView { artifactView ->
                                artifactView.componentFilter { componentIdentifier ->
                                    componentIdentifier is ProjectComponentIdentifier
                                }
                            }
                            .artifacts
                            .filter {
                                it.id `is` CompositeProjectComponentArtifactMetadata
                            }
                            .map { it.file }
                            .toSet()
                    }
                ).disallowChanges()

                task.toolsNpmDependencies.value(
                    project.provider {
                        nodeJsRoot
                            .taskRequirements
                            .getCompilationNpmRequirements(projectPath, compilationDisambiguatedName)
                            .map { it.toString() }
                            .sorted()
                    }
                ).disallowChanges()

                task.onlyIf {
                    it as KotlinPackageJsonTask
                    it.npmResolutionManager.get().isConfiguringState()
                }

                task.dependsOn(
                    target.project.provider {
                        findDependentTasks(
                            nodeJsRoot.resolver,
                            getCompilationResolver(
                                nodeJsRoot,
                                projectPath,
                                compilationDisambiguatedName
                            ).compilationNpmResolution
                        )
                    }
                )

                task.dependsOn(nodeJsRoot.npmCachesSetupTaskProvider)
            }

            nodeJsRoot.packageJsonUmbrellaTaskProvider.configure { task ->
                task.inputs.file(packageJsonTask.map { it.packageJson })
            }

            nodeJsRoot.rootPackageJsonTaskProvider
                .configure {
                    it.mustRunAfter(packageJsonTask)
                }

            return packageJsonTask
        }

        private fun findDependentTasks(
            rootResolver: KotlinRootNpmResolver,
            compilationNpmResolution: KotlinCompilationNpmResolution,
        ): Collection<Any> =
            compilationNpmResolution.internalDependencies.map { dependency ->
                rootResolver[dependency.projectPath][dependency.compilationName].npmProject.packageJsonTaskPath
            } + compilationNpmResolution.internalCompositeDependencies.map { dependency ->
                dependency.includedBuild?.task(":$PACKAGE_JSON_UMBRELLA_TASK_NAME") ?: error("includedBuild instance is not available")
                dependency.includedBuild.task(":${RootPackageJsonTask.NAME}")
            }

        private fun getCompilationResolver(
            nodeJsRoot: BaseNodeJsRootExtension,
            projectPath: String,
            compilationDisambiguatedName: String,
        ): KotlinCompilationNpmResolver =
            nodeJsRoot.resolver[projectPath][compilationDisambiguatedName]
    }
}