/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinImportMapGenerateTask
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

internal class DevServerConfigurator(
    private val subTarget: KotlinJsIrSubTarget,
) : SubTargetConfigurator<KotlinSimpleDevServerTask, KotlinSimpleDevServerTask> {

    private val project = subTarget.project

    private val runTaskConfigurations = project.objects.domainObjectSet<Action<KotlinSimpleDevServerTask>>()

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
        // Dev server doesn't produce build artifacts
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {
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

                    task.host.convention("localhost")

                    configureImportMap(task, compilation)

                    runTaskConfigurations.all {
                        it.execute(task)
                    }
                }
            }
    }

    private fun configureImportMap(task: KotlinSimpleDevServerTask, compilation: KotlinJsIrCompilation) {
        val importMapTaskName = compilation.disambiguateName("importMap")
        val importMapTask = project.tasks.findByName(importMapTaskName) as? KotlinImportMapGenerateTask ?: return

        task.dependsOn(importMapTask)
        task.importMapFile.set(importMapTask.importMapFile)

        val nodeJsRoot = subTarget.target.webTargetVariant(
            { project.rootProject.kotlinNodeJsRootExtension },
            { project.rootProject.wasmKotlinNodeJsRootExtension },
        )
        task.npmRootDirectory.set(nodeJsRoot.rootPackageDirectory)
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
