/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.isolated

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.fixSemver
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.packageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.toDeclaration
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.npm.internal.collectEmbeddedNpmDependencies
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.createResolvableCompilationNpmPackageJsonFilesConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.npmDependenciesCollector
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.getFile
import javax.inject.Inject

/**
 * Produces the `package.json` of a single Kotlin/JS compilation using project-local information only.
 *
 * Unlike [org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask], this task does not
 * read the npm resolution of the whole build from the root project, so it is compatible with
 * Gradle Isolated Projects.
 *
 * The produced `package.json` becomes an npm workspace of the shared npm root project,
 * which is assembled by the project that applies `KotlinSharedNpmProjectPlugin`.
 */
@CacheableTask
internal abstract class KotlinIsolatedPackageJsonTask : DefaultTask() {

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    /**
     * The npm package name of the compilation, which is also the name of its npm workspace.
     */
    @get:Input
    abstract val npmProjectName: Property<String>

    @get:Input
    abstract val npmProjectVersion: Property<String>

    @get:Input
    abstract val packageJsonMain: Property<String>

    @get:Optional
    @get:Input
    abstract val packageJsonTypes: Property<String>

    /**
     * The npm dependencies declared by the source sets of the compilation.
     */
    @get:Input
    abstract val declaredNpmDependencies: ListProperty<KotlinNpmDependency>

    /**
     * The npm dependencies required by the tasks of the compilation, such as `mocha` for a test task.
     */
    @get:Input
    abstract val toolsNpmDependencies: ListProperty<NpmDependencyDeclaration>

    /**
     * The Kotlin/JS libraries of the compilation, which may carry their own `package.json`.
     *
     * @see collectEmbeddedNpmDependencies
     */
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val runtimeClasspath: ConfigurableFileCollection

    /**
     * The `package.json` files of the projects this compilation depends on at runtime.
     *
     * Their npm dependencies are merged into the `package.json` of this compilation,
     * so that the npm dependencies declared by a project are visible to the projects depending on it.
     * They are transitive as well, because the `package.json` of a project already contains
     * the npm dependencies of the projects it depends on.
     */
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val dependenciesPackageJsonFiles: ConfigurableFileCollection

    @get:Internal
    abstract val packageJsonHandlers: ListProperty<Action<PackageJson>>

    /**
     * The effect of [packageJsonHandlers] on the resulting `package.json`,
     * so that the task re-runs when the user changes them.
     */
    @get:Input
    abstract val packageJsonInputHandlers: Property<PackageJson>

    @get:OutputFile
    abstract val packageJsonFile: RegularFileProperty

    @TaskAction
    fun createPackageJson() {
        val npmDependencies = declaredNpmDependencies.get().map { it.toNpmDependencyDeclaration() } +
                toolsNpmDependencies.get() +
                collectEmbeddedNpmDependencies(archiveOperations, dependenciesPackageJsonFiles) +
                collectEmbeddedNpmDependencies(archiveOperations, runtimeClasspath)

        val packageJson = packageJson(
            name = npmProjectName.get(),
            version = npmProjectVersion.get(),
            main = packageJsonMain.get(),
            types = packageJsonTypes.orNull,
            npmDependencies = npmDependencies.deduplicate(),
            packageJsonHandlers = packageJsonHandlers.get(),
        )

        packageJsonHandlers.get().forEach { it.execute(packageJson) }

        packageJson.saveTo(packageJsonFile.getFile())
    }

    /**
     * Keeps the first declaration of every npm package, so that the dependencies declared by the user
     * win over the ones coming from the tasks and from the Kotlin/JS libraries.
     */
    private fun Collection<NpmDependencyDeclaration>.deduplicate(): Collection<NpmDependencyDeclaration> =
        distinctBy { it.name }

    private fun KotlinNpmDependency.toNpmDependencyDeclaration(): NpmDependencyDeclaration =
        NpmDependencyDeclaration(
            scope = when (scope) {
                KotlinNpmDependency.Scope.NORMAL -> NpmDependency.Scope.NORMAL
                KotlinNpmDependency.Scope.DEV -> NpmDependency.Scope.DEV
                KotlinNpmDependency.Scope.OPTIONAL -> NpmDependency.Scope.OPTIONAL
                KotlinNpmDependency.Scope.PEER -> NpmDependency.Scope.PEER
            },
            name = name,
            version = version,
        )

    companion object {
        fun register(compilation: KotlinJsIrCompilation, platform: HasPlatformDisambiguator) {
            val project = compilation.project
            val npmProject = compilation.npmProject
            val objects = project.objects

            val dependenciesPackageJsonFiles =
                createResolvableCompilationNpmPackageJsonFilesConfiguration(compilation, platform)
                    .incoming
                    .artifactView { it.isLenient = true }
                    .files

            project.registerTask<KotlinIsolatedPackageJsonTask>(npmProject.packageJsonTaskName) { task ->
                task.description = "Create package.json file for ${compilation.name} compilation"

                val npmProjectVersion = fixSemver(project.version.toString())

                task.npmProjectName.value(npmProject.name).disallowChanges()
                task.npmProjectVersion.value(npmProjectVersion).disallowChanges()
                task.packageJsonMain.value(npmProject.main).disallowChanges()
                task.packageJsonTypes.value(npmProject.typesFilePath).disallowChanges()
                task.packageJsonFile.value(npmProject.packageJsonFile).disallowChanges()

                task.packageJsonHandlers.value(project.provider { compilation.packageJsonHandlers }).disallowChanges()
                task.packageJsonInputHandlers.value(
                    task.npmProjectName.zip(task.packageJsonHandlers) { name, handlers ->
                        PackageJson(name, npmProjectVersion)
                            .also { fakePackageJson -> handlers.forEach { it.execute(fakePackageJson) } }
                    }
                ).disallowChanges()

                // The source sets of the associated compilations are visible to this compilation,
                // so the npm dependencies they declare are needed by it as well:
                // for example, the npm dependencies of `jsMain` are needed by the `jsTest` compilation.
                val sourceSets = compilation.allKotlinSourceSets +
                        getVisibleSourceSetsFromAssociateCompilations(compilation.defaultSourceSet)

                sourceSets.forEach { sourceSet ->
                    task.declaredNpmDependencies.addAll(sourceSet.internal.npmDependenciesCollector.npmDependencies)
                }
                task.declaredNpmDependencies.disallowChanges()

                task.toolsNpmDependencies.value(
                    project.provider {
                        project.tasks.withType<RequiresNpmDependenciesTask>()
                            .filter { it.enabled && it.compilation === compilation }
                            .flatMap { it.requiredNpmDependencies }
                            .map { it.createDependency(objects) }
                            .filterIsInstance<NpmDependency>()
                            .map { it.toDeclaration() }
                    }
                ).disallowChanges()

                task.runtimeClasspath.from(compilation.runtimeDependencyFiles)
                task.runtimeClasspath.disallowChanges()

                task.dependenciesPackageJsonFiles.from(dependenciesPackageJsonFiles)
                task.dependenciesPackageJsonFiles.disallowChanges()
            }
        }
    }
}
