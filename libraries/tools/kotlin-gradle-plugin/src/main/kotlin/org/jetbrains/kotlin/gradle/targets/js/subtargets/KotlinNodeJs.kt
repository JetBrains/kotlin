/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinNodeJsIr
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import javax.inject.Inject

open class KotlinNodeJs @Inject constructor(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "node"),
    KotlinJsNodeDsl {
    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    private val runTaskName = disambiguateCamelCased("run")

    private val irNodejs: KotlinNodeJsIr?
        get() = target.irTarget?.nodejs

    override fun produceKotlinLibrary() {
        super.produceKotlinLibrary()
        irNodejs?.produceKotlinLibrary()
    }

    override fun produceExecutable() {
        super.produceExecutable()
        irNodejs?.produceExecutable()
    }

    override fun runTask(body: NodeJsExec.() -> Unit) {
        (project.tasks.getByName(runTaskName) as NodeJsExec).body()
        irNodejs?.runTask(body)
    }

    override fun testTask(body: KotlinJsTest.() -> Unit) {
        super<KotlinJsSubTarget>.testTask(body)
        irNodejs?.testTask(body)
    }

    override fun configureDefaultTestFramework(testTask: KotlinJsTest) {
        testTask.useMocha { }
    }

    override fun configureMain(compilation: KotlinJsCompilation) {
        configureRun(compilation)
    }

    private fun configureRun(
        compilation: KotlinJsCompilation
    ) {
        val runTaskHolder = NodeJsExec.create(compilation, disambiguateCamelCased(RUN_TASK_NAME)) {
            inputFileProperty.set(compilation.compileKotlinTask.outputFile)
        }
        target.runTask.dependsOn(runTaskHolder)
    }

    override fun configureBuildVariants() {
    }
}