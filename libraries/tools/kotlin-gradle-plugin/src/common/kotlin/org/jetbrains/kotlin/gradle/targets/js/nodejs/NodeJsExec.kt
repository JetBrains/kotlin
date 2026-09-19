/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.dependsOnNpmTooling
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.wasm.internal.isWasm
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.nodejs.nodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.nodejs.npmVersions
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.UsesKotlinAggregatedNpmWorkspaceService
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.isIsolatedNpmResolutionEnabled
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import javax.inject.Inject

@DisableCachingByDefault
abstract class NodeJsExec
@Inject
constructor(
    @Internal
    @Transient
    final override val compilation: KotlinJsIrCompilation,
) : AbstractExecTask<NodeJsExec>(NodeJsExec::class.java), RequiresNpmDependenciesTask, UsesKotlinAggregatedNpmWorkspaceService {

    @get:Internal
    internal abstract val versions: Property<NpmVersions>

    @Internal
    val npmProject = compilation.npmProject

    init {
        this.onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map {
                it.exists()
            }.get()
        }
    }

    @get:Internal
    internal abstract val npmToolingEnvDir: DirectoryProperty

    /**
     * The name of the npm workspace of the compilation, set only when the Isolated Projects compatible
     * NPM resolution is enabled: in that case both the executed files and the node modules
     * come from the npm workspace owned by [org.jetbrains.kotlin.gradle.targets.web.npm.isolated.KotlinAggregatedNpmWorkspaceService].
     */
    @get:Internal
    internal abstract val npmWorkspaceName: Property<String>

    @Input
    var nodeArgs: MutableList<String> = mutableListOf()

    @Input
    var sourceMapStackTraces = true

    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() =
            if (sourceMapStackTraces) {
                setOf(versions.get().sourceMapSupport)
            } else {
                emptySet()
            }

    override fun exec() {
        val npmWorkspaceName = npmWorkspaceName.orNull
        val npmWorkspace = npmWorkspaceName?.let { aggregatedNpmWorkspaceService.get() }

        val newArgs = mutableListOf<String>()
        newArgs.addAll(nodeArgs)
        if (inputFileProperty.isPresent) {
            val inputFile = inputFileProperty.asFile.get().normalize()
            // The files are executed from the npm workspace of the compilation,
            // where its npm dependencies are installed and configured.
            val executedFile = if (npmWorkspace != null) {
                npmWorkspace.npmProjectDistDirectory(npmWorkspaceName).resolve(inputFile.name)
            } else {
                inputFile
            }
            newArgs.add(executedFile.absolutePath)
        }
        args?.let { newArgs.addAll(it) }
        args = newArgs

        val modules = if (npmWorkspace != null) {
            npmWorkspace.npmProjectModules(npmWorkspaceName).also { workingDir = it.dir }
        } else {
            NpmProjectModules(npmToolingEnvDir.getFile())
        }

        if (sourceMapStackTraces) {
            val sourceMapSupportArgs = mutableListOf(
                "--require",
                modules.require("source-map-support/register.js")
            )

            args?.let { sourceMapSupportArgs.addAll(it) }

            args = sourceMapSupportArgs
        }

        super.exec()
    }

    companion object {
        fun register(
            compilation: KotlinJsIrCompilation,
            name: String,
            configuration: NodeJsExec.() -> Unit = {},
        ): TaskProvider<NodeJsExec> {
            val target = compilation.target
            val project = target.project

            val npmVersions = compilation.npmVersions
            val nodeJsEnvSpec = compilation.nodeJsEnvSpec

            val npmProject = compilation.npmProject
            val npmToolingDir: Provider<Directory> = compilation.npmToolingDir()

            return project.registerTask(
                name,
                listOf(compilation)
            ) {
                it.versions.value(npmVersions)
                    .disallowChanges()
                it.executable = nodeJsEnvSpec.executable.get()
                if (compilation.target.wasmTargetType != KotlinWasmTargetType.WASI) {
                    it.workingDir(npmProject.dir)
                    it.dependsOnNpmTooling(compilation)
                }

                it.npmToolingEnvDir.set(npmToolingDir)
                it.npmToolingEnvDir.disallowChanges()

                if (project.isIsolatedNpmResolutionEnabled && compilation.wasmTarget == null) {
                    it.npmWorkspaceName.set(npmProject.name)
                }
                it.npmWorkspaceName.disallowChanges()

                with(nodeJsEnvSpec) {
                    it.dependsOn(project.nodeJsSetupTaskProvider)
                }
                it.dependsOn(compilation.compileTaskProvider)
                it.configuration()
            }
        }
    }
}
