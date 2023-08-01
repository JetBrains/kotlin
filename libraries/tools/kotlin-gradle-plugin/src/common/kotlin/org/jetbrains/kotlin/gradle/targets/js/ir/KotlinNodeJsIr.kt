/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmNode
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.withType
import javax.inject.Inject

abstract class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTargetBase(target, "node"),
    KotlinJsNodeDsl {

    private val nodeJs = project.rootProject.kotlinNodeJsExtension
    private val nodeJsTaskProviders = project.rootProject.kotlinNodeJsExtension

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    override fun runTask(body: Action<NodeJsExec>) {
        project.tasks.withType<NodeJsExec>().named(runTaskName).configure(body)
    }

    override fun configureRun(compilation: KotlinJsIrCompilation) {
        if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            super.configureRun(compilation)
        }
    }

    override fun locateOrRegisterRunTask(binary: JsIrBinary, name: String) {
        if (project.locateTask<NodeJsExec>(name) != null) return

        val runTaskHolder = NodeJsExec.create(binary.compilation, name) {
            group = taskGroupName
            dependsOn(binary.linkSyncTask)
            inputFileProperty.fileProvider(
                binary.linkSyncTask.flatMap { linkSyncTask ->
                    binary.linkTask.flatMap { linkTask ->
                        linkTask.outputFileProperty.map { file ->
                            linkSyncTask.destinationDirectory.get().resolve(file.name)
                        }
                    }
                }
            )
        }
        target.runTask.dependsOn(runTaskHolder)
    }

    override fun configureTestDependencies(test: KotlinJsTest) {
        if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            test.dependsOn(
                nodeJsTaskProviders.npmInstallTaskProvider,
                nodeJsTaskProviders.storeYarnLockTaskProvider,
                nodeJsTaskProviders.nodeJsSetupTaskProvider
            )
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