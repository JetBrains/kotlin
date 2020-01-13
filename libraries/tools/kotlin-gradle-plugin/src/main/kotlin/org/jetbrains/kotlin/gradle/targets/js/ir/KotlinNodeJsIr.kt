/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsIrNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import javax.inject.Inject

open class KotlinNodeJsIr @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "node"),
    KotlinJsIrNodeDsl {
    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    private val runTaskName = disambiguateCamelCased("run")

    override fun configure() {
        super.configure()
        nodejsProducingConfiguredHandlers.forEach { handler ->
            handler(this)
        }
    }

    override fun runTask(body: NodeJsExec.() -> Unit) {
        (project.tasks.getByName(runTaskName) as NodeJsExec).body()
    }

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useMocha { }
    }

    override fun configureMain(compilation: KotlinJsIrCompilation) {
        configureRun(compilation)
    }

    private fun configureRun(
        compilation: KotlinJsIrCompilation
    ) {
        val runTaskHolder = NodeJsExec.create(compilation, disambiguateCamelCased(RUN_TASK_NAME)) {
            dependsOn(compilation.developmentLinkTaskName)
            args(compilation.developmentLinkTask.outputFile)
        }
        target.runTask.dependsOn(runTaskHolder)
    }

    override fun configureBuildVariants() {
    }

    fun whenProducingConfigured(body: KotlinNodeJsIr.() -> Unit) {
        if (producingConfigured) {
            this.body()
        } else {
            nodejsProducingConfiguredHandlers += body
        }
    }
}