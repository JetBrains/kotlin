/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import javax.inject.Inject

open class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "node"),
    KotlinJsNodeDsl {
    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    private val runTaskName = disambiguateCamelCased("run")

    override fun runTask(body: NodeJsExec.() -> Unit) {
        (project.tasks.getByName(runTaskName) as NodeJsExec).body()
    }

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useMocha { }
    }

    override fun configureRun(
        compilation: KotlinJsIrCompilation
    ) {
        val developmentExecutable = compilation.binaries.getBinary(KotlinJsBinaryType.DEVELOPMENT)

        val runTaskHolder = NodeJsExec.create(compilation, disambiguateCamelCased(RUN_TASK_NAME)) {
            inputFileProperty.set(developmentExecutable.linkTask.map { it.outputFileProperty.get() })
        }
        target.runTask.dependsOn(runTaskHolder)
    }

    override fun configureBuild(
        compilation: KotlinJsIrCompilation
    ) {
        val productionExecutable = compilation.binaries.getBinary(KotlinJsBinaryType.PRODUCTION)

        val assembleTask = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        assembleTask.dependsOn(productionExecutable.linkTask)
    }
}