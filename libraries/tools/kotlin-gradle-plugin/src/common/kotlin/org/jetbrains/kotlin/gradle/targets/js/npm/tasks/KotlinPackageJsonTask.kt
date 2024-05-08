/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.mapToFile
import org.jetbrains.kotlin.gradle.utils.setProperty
import java.io.File

@DisableCachingByDefault
abstract class KotlinPackageJsonTask :
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

    @get:Internal
    val compilationFile: Property<RegularFile> = project.objects.fileProperty()

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

    @get:Input
    internal val externalNpmDependencies: SetProperty<NpmDependencyDeclaration> = project.objects.setProperty<NpmDependencyDeclaration>()

    @get:OutputFile
    abstract val packageJson: Property<File>

    @TaskAction
    fun resolve() {
        val resolution = npmResolutionManager.get().resolution.get()[projectPath][compilationFile.getFile()]
        val preparedResolution = resolution
            .resolution!!

        resolution.createPackageJson(preparedResolution, packageJsonMain, packageJsonHandlers)
    }

    companion object {
        internal fun create(
            compilation: KotlinJsIrCompilation,
            disambiguatedName: String,
            compilationResolution: Provider<KotlinCompilationNpmResolution>
        ): TaskProvider<KotlinPackageJsonTask> {
            val project = compilation.target.project
            val npmProject = compilation.npmProject
            val nodeJsTaskProviders = project.rootProject.kotlinNodeJsExtension

            val npmCachesSetupTask = nodeJsTaskProviders.npmCachesSetupTaskProvider
            val packageJsonTaskName = npmProject.packageJsonTaskName
            val packageJsonUmbrella = nodeJsTaskProviders.packageJsonUmbrellaTaskProvider

            val npmResolutionManager = project.kotlinNpmResolutionManager
            val gradleNodeModules = GradleNodeModulesCache.registerIfAbsent(project, null, null)
            val packageJsonTask = project.registerTask<KotlinPackageJsonTask>(packageJsonTaskName) { task ->
                task.compilationDisambiguatedName.set(disambiguatedName)
                task.compilationFile.set(npmProject.publicPackageJsonFile)
                task.packageJsonHandlers.set(compilation.packageJsonHandlers)
                task.description = "Create package.json file for $compilation"
                task.group = NodeJsRootPlugin.TASKS_GROUP_NAME
                task.externalNpmDependencies.set(
                    compilationResolution.map { it.npmDependencies }
                )

                task.npmResolutionManager.value(npmResolutionManager)
                    .disallowChanges()

                task.gradleNodeModules.value(gradleNodeModules)
                    .disallowChanges()

                task.packageJsonMain.set(compilation.npmProject.main)

                task.packageJson.set(compilation.npmProject.packageJsonFile.mapToFile())

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
    }
}