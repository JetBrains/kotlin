/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmNode
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.mapToFile
import javax.inject.Inject

abstract class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "node"),
    KotlinJsNodeDsl {

    init {
        if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            target.compilations.all { compilation ->
                compilation.binaries.all { binary ->
                    if (project.locateTask<IncrementalSyncTask>(binary.npmProjectLinkSyncTaskName()) == null) {

                        project.registerTask<DefaultIncrementalSyncTask>(
                            binary.npmProjectLinkSyncTaskName()
                        ) { task ->
                            fun fromLinkTask() {
                                task.from.from(
                                    binary.linkTask.flatMap { linkTask ->
                                        linkTask.destinationDirectory
                                    }
                                )
                            }
                            when (binary) {
                                is ExecutableWasm -> {
                                    if (compilation.isMain() && binary.mode == KotlinJsBinaryMode.PRODUCTION) {
                                        task.from.from(binary.optimizeTask.flatMap { it.outputFileProperty.map { it.asFile.parentFile } })
                                        task.dependsOn(binary.optimizeTask)
                                    } else {
                                        fromLinkTask()
                                    }
                                }
                                is LibraryWasm -> {
                                    if (compilation.isMain() && binary.mode == KotlinJsBinaryMode.PRODUCTION) {
                                        task.from.from(binary.optimizeTask.flatMap { it.outputFileProperty.map { it.asFile.parentFile } })
                                        task.dependsOn(binary.optimizeTask)
                                    } else {
                                        fromLinkTask()
                                    }
                                }
                                else -> {
                                    fromLinkTask()
                                }
                            }

                            task.duplicatesStrategy = DuplicatesStrategy.WARN

                            task.from.from(project.tasks.named(binary.compilation.processResourcesTaskName))

                            task.destinationDirectory.set(binary.compilation.npmProject.dist.mapToFile())
                        }
                    }
                }
            }
        }
    }

    private val nodeJs = project.rootProject.kotlinNodeJsExtension
    private val nodeJsTaskProviders = project.rootProject.kotlinNodeJsExtension

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    override fun runTask(body: Action<NodeJsExec>) {
        subTargetConfigurators.configureEach {
            if (it is NodeJsEnvironmentConfigurator) {
                it.configureRun(body)
            }
        }
    }

    @ExperimentalMainFunctionArgumentsDsl
    override fun passProcessArgvToMainFunction() {
        target.passAsArgumentToMainFunction("process.argv")
    }

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        test.dependsOn(nodeJsTaskProviders.nodeJsSetupTaskProvider)
        if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            test.dependsOn(
                nodeJsTaskProviders.npmInstallTaskProvider,
            )
            test.dependsOn(nodeJs.packageManagerExtension.map { it.postInstallTasks })
            test.dependsOn(binary.npmProjectLinkSyncTaskName())
        }
        test.dependsOn(binary.linkTask)
    }

    override fun mainInputFile(binary: JsIrBinary): Provider<RegularFile> {
        return if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            binary.npmProjectMainFileSyncPath()
        } else {
            binary.mainFile
        }
    }

    override fun testInputFile(binary: JsIrBinary): Provider<RegularFile> {
        return if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            binary.npmProjectMainFileSyncPath()
        } else {
            binary.mainFile
        }
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        if (target.platformType != KotlinPlatformType.wasm) {
            if (test.testFramework == null) {
                test.useMocha { }
            }
            if (test.enabled) {
                nodeJs.taskRequirements.addTaskRequirements(test)
            }
        } else {
            test.testFramework = KotlinWasmNode(test)
        }
    }
}