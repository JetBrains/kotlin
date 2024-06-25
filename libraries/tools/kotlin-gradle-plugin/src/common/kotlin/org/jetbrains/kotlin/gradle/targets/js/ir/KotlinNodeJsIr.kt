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
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmNode
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.named
import javax.inject.Inject

abstract class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrNpmBasedSubTarget(target, "node"),
    KotlinJsNodeDsl {

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