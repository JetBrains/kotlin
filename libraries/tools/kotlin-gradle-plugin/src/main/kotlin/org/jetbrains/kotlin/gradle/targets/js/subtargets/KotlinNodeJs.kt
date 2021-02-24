/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.withType
import javax.inject.Inject

open class KotlinNodeJs @Inject constructor(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "node"),
    KotlinJsNodeDsl {
    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    private val runTaskName = disambiguateCamelCased("run")

    override fun runTask(body: NodeJsExec.() -> Unit) {
        project.tasks.withType<NodeJsExec>().named(runTaskName).configure(body)
    }

    @ExperimentalDistributionDsl
    override fun distribution(body: Distribution.() -> Unit) {
        TODO("Not yet implemented")
    }

    override fun testTask(body: KotlinJsTest.() -> Unit) {
        super<KotlinJsSubTarget>.testTask(body)
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
            group = taskGroupName
            inputFileProperty.set(project.layout.file(compilation.compileKotlinTaskProvider.map { it.outputFile }))
        }
        target.runTask.dependsOn(runTaskHolder)
    }
}