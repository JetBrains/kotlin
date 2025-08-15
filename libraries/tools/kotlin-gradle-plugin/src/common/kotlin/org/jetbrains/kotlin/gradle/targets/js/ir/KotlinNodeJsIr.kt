/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmNode
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.withType
import javax.inject.Inject

abstract class KotlinNodeJsIr
@Inject
internal constructor(
    target: KotlinJsIrTarget,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) :
    KotlinJsIrNpmBasedSubTarget(target, "node"),
    KotlinJsNodeDsl {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    constructor(
        target: KotlinJsIrTarget,
    ) : this(
        target = target,
        objects = target.project.objects,
        providers = target.project.providers,
    )

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

    @ExperimentalMainFunctionArgumentsDsl
    override fun passCliArgumentsToMainFunction() {
        target.passAsArgumentToMainFunction("process.argv.slice(2)")
    }

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        with(nodeJsEnvSpec) {
            test.dependsOn(project.nodeJsSetupTaskProvider)
        }

        if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
            test.dependsOn(
                nodeJsRoot.npmInstallTaskProvider,
            )
            if (target.webTargetVariant(jsVariant = false, wasmVariant = true)) {
                test.dependsOn((nodeJsRoot as WasmNodeJsRootExtension).toolingInstallTaskProvider)
            }
            test.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })
            test.dependsOn(binary.linkSyncTask)
        }
        test.dependsOn(binary.linkTask)
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        if (target.platformType != KotlinPlatformType.wasm) {
            if (test.testFramework == null) {
                test.useMocha { }
            }
            if (test.enabled) {
                nodeJsRoot.taskRequirements.addTaskRequirements(test)
            }
        } else {
            test.testFramework = KotlinWasmNode(test, objects, providers)
        }
    }

    internal companion object {
        internal const val SWC_TASK_NAME = "swc"
    }
}
