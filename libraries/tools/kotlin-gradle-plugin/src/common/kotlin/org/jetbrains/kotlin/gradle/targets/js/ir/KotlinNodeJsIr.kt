/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmNode
import org.jetbrains.kotlin.gradle.tasks.withType
import javax.inject.Inject

open class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTargetBase(target, "node"),
    KotlinJsNodeDsl {

    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    override fun runTask(body: NodeJsExec.() -> Unit) {
        project.tasks.withType<NodeJsExec>().named(runTaskName).configure(body)
    }

    override fun configureTestDependencies(test: KotlinJsTest) {
        test.dependsOn(nodeJs.npmInstallTaskProvider, nodeJs.nodeJsSetupTaskProvider)
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

    override val additionalCompilerOption: String?
        get() = "-Xwasm-launcher=nodejs".takeIf { target.platformType == KotlinPlatformType.wasm }
}