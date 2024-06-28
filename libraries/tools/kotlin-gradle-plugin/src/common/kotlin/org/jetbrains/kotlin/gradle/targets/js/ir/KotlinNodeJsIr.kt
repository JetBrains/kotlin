/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmNode
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.withType
import javax.inject.Inject

abstract class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTargetBase(target, "node"),
    KotlinJsNodeDsl {

    private val nodeJs = project.kotlinNodeJsExtension

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    override fun runTask(body: Action<NodeJsExec>) {
        project.tasks.withType<NodeJsExec>().named(runTaskName).configure(body)
    }

    @ExperimentalMainFunctionArgumentsDsl
    override fun passProcessArgvToMainFunction() {
        target.passAsArgumentToMainFunction("process.argv")
    }

    override fun locateOrRegisterRunTask(binary: JsIrBinary, name: String) {
        if (project.locateTask<NodeJsExec>(name) != null) return

        val compilation = binary.compilation
        val runTaskHolder = NodeJsExec.create(compilation, name) {
            group = taskGroupName
            val inputFile = if ((compilation.target as KotlinJsIrTarget).wasmTargetType == KotlinWasmTargetType.WASI) {
                sourceMapStackTraces = false
                if (binary is ExecutableWasm && binary.mode == KotlinJsBinaryMode.PRODUCTION) {
                    dependsOn(binary.optimizeTask)
                    binary.mainOptimizedFile
                } else {
                    dependsOn(binary.linkTask)
                    binary.mainFile
                }
            } else {
                dependsOn(binary.linkSyncTask)
                binary.mainFileSyncPath
            }
            inputFileProperty.set(
                inputFile
            )
        }
        target.runTask.dependsOn(runTaskHolder)
    }

    override fun configureTestDependencies(test: KotlinJsTest) {
        test.dependsOn(nodeJs.nodeJsSetupTaskProvider)
        if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            val nodeJsRoot = project.rootProject.kotlinNodeJsRootExtension
            test.dependsOn(
                nodeJsRoot.npmInstallTaskProvider,
            )
            test.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })
        }
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        if (target.platformType != KotlinPlatformType.wasm) {
            val nodeJsRoot = project.rootProject.kotlinNodeJsRootExtension
            if (test.testFramework == null) {
                test.useMocha { }
            }
            if (test.enabled) {
                nodeJsRoot.taskRequirements.addTaskRequirements(test)
            }
        } else {
            test.testFramework = KotlinWasmNode(test)
        }
    }
}