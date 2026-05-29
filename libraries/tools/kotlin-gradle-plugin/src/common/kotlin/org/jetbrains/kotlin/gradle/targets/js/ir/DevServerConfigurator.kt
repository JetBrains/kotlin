/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.Companion.DISTRIBUTION_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.ir.SimpleDistributionTask.Companion.VENDORS_FOLDER
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinImportMapGenerateTask
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

internal class DevServerConfigurator(
    private val subTarget: KotlinJsIrSubTarget,
) : SubTargetConfigurator<KotlinSimpleDevServerTask, KotlinSimpleDevServerTask> {

    private val project = subTarget.project

    private val nodeJsRoot = subTarget.target.webTargetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    private val runTaskConfigurations = project.objects.domainObjectSet<Action<KotlinSimpleDevServerTask>>()

    private lateinit var importMapTaskHolder: TaskProvider<KotlinImportMapGenerateTask>

    private fun importMapTask(compilation: KotlinJsIrCompilation): TaskProvider<KotlinImportMapGenerateTask> {
        if (::importMapTaskHolder.isInitialized) return importMapTaskHolder

        val npmProject = compilation.npmProject

        importMapTaskHolder = project.registerTask<KotlinImportMapGenerateTask>(npmProject.generateImportMapTaskName) {
            it.nodeModulesDirectory.set(npmProject.nodeModulesDir)
            it.rootDirectory.set(project.rootDir)
            it.installArtifacts.from(nodeJsRoot.npmInstallTaskProvider.map { it.additionalFiles })
            it.inputDirectory.set(npmProject.dir)
            it.importMapFile.set(project.layout.buildDirectory.file("tmp/${it.name}/importmap.json"))
            it.importMapLoaderFile.set(project.layout.buildDirectory.file("tmp/${it.name}/importmap-loader.js"))
        }

        return importMapTaskHolder
    }

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
        val npmProject = compilation.npmProject

        val importMapTaskHolder = importMapTask(compilation)

        compilation.binaries
            .withType<Executable>()
            .configureEach { binary ->
                val mode = binary.mode
                if (mode != KotlinJsBinaryMode.PRODUCTION) return@configureEach

                val importMapDistTaskHolder = project.registerTask<KotlinImportMapGenerateTask>(npmProject.generateImportMapDistTaskName) {
                    it.nodeModulesDirectory.set(npmProject.nodeModulesDir)
                    it.rootDirectory.set(project.rootDir)
                    it.installArtifacts.from(nodeJsRoot.npmInstallTaskProvider.map { it.additionalFiles })
                    it.inputDirectory.set(npmProject.dir)
                    it.dependsOn(binary.linkSyncTask)
                    it.importMapFile.set(project.layout.buildDirectory.file("tmp/${it.name}/importmap.json"))
                    it.importMapLoaderFile.set(project.layout.buildDirectory.file("tmp/${it.name}/importmap-loader.js"))
                    it.flattenPaths.set(true)
                    it.pathPrefix.set("./$VENDORS_FOLDER")
                }

                subTarget.registerSubTargetTask<SimpleDistributionTask>(
                    subTarget.disambiguateCamelCased(
                        if (binary.mode == KotlinJsBinaryMode.PRODUCTION && binary.compilation.isMain())
                            ""
                        else
                            binary.name,
                        "Simple",
                        DISTRIBUTION_TASK_NAME,
                    )
                ) {
                    it.mainDirectory.fileProvider(binary.linkSyncTask.flatMap { it.destinationDirectory })
                    it.importMapLoader.set(importMapDistTaskHolder.flatMap { it.importMapLoaderFile })
                    it.importMapFile.set(importMapTaskHolder.flatMap { it.importMapFile })
                    it.outputDirectory.set(binary.distribution.outputDirectory)
                }
            }
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {

        val importMapTaskHolder = importMapTask(compilation)

        compilation.binaries
            .withType<Executable>()
            .configureEach { binary ->
                val mode = binary.mode
                if (mode != KotlinJsBinaryMode.DEVELOPMENT) return@configureEach

                val linkSyncTask = binary.linkSyncTask

                subTarget.registerSubTargetTask<KotlinSimpleDevServerTask>(
                    subTarget.disambiguateCamelCased(
                        binary.executeTaskBaseName,
                        DEV_SERVER_TASK_NAME
                    )
                ) { task ->
                    task.description = "start a simple development server serving ${mode.name.toLowerCaseAsciiOnly()} files"

                    task.contentDirectory.fileProvider(
                        linkSyncTask.flatMap { it.destinationDirectory }
                    )

                    runTaskConfigurations.all {
                        it.execute(task)
                    }
                }

                binary.linkSyncTask.configure { syncTask ->
                    syncTask.from.from(importMapTaskHolder.map { it.importMapLoaderFile })
                }
            }
    }

    override fun configureBuild(body: Action<KotlinSimpleDevServerTask>) {
        // Dev server doesn't produce build artifacts
    }

    override fun configureRun(body: Action<KotlinSimpleDevServerTask>) {
        runTaskConfigurations.add(body)
    }

    companion object {
        const val DEV_SERVER_TASK_NAME = "devServer"
    }
}
