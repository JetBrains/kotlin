/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmNode
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.withType
import javax.inject.Inject

abstract class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrNpmBasedSubTarget(target, "node"),
    KotlinJsNodeDsl {

    private val nodeJs = project.kotlinNodeJsEnvSpec

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    override fun runTask(body: Action<NodeJsExec>) {
        subTargetConfigurators
            .withType<NodeJsEnvironmentConfigurator>()
            .configureEach {
                it.configureRun(body)
            }
    }

    @ExperimentalMainFunctionArgumentsDsl
    override fun passProcessArgvToMainFunction() {
        target.passAsArgumentToMainFunction("process.argv")
    }

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        with(nodeJs) {
            test.dependsOn(project.nodeJsSetupTaskProvider)
        }
        if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            val nodeJsRoot = project.rootProject.kotlinNodeJsRootExtension
            test.dependsOn(
                nodeJsRoot.npmInstallTaskProvider,
            )
            test.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })
            test.dependsOn(binary.linkSyncTask)
        }
        test.dependsOn(binary.linkTask)
    }

    override fun binaryInputFile(binary: JsIrBinary): Provider<RegularFile> {
        return if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            super.binaryInputFile(binary)
        } else {
            project.objects.fileProperty().fileProvider(
                project.tasks.named<IncrementalSyncTask>(binarySyncTaskName(binary)).map {
                    it.destinationDirectory.get().resolve(binary.mainFileName.get())
                }
            )
        }
    }

    override fun binarySyncTaskName(binary: JsIrBinary): String {
        return if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            super.binarySyncTaskName(binary)
        } else {
            disambiguateCamelCased(
                binary.compilation.name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
                binary.name,
                COMPILE_SYNC
            )
        }
    }

    override fun binarySyncOutput(binary: JsIrBinary): Provider<Directory> {
        return if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            super.binarySyncOutput(binary)
        } else {
            project.objects.directoryProperty().fileProvider(
                binary.linkTask.map { it.destinationDirectory.getFile().parentFile.resolve(disambiguationClassifier) }
            )
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