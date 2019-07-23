/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

class KotlinNodeJs(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "node"),
    KotlinJsNodeDsl {

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside nodejs using the builtin test framework"

    override fun runTask(body: NodeJsExec.() -> Unit) {
        (project.tasks.getByName(runTaskName) as NodeJsExec).body()
    }

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useNodeJs { }
    }

    override fun configureRun(compilation: KotlinJsCompilation) {
        val runTaskHolder = NodeJsExec.create(compilation, disambiguateCamelCased("run"))
        target.runTask.dependsOn(runTaskHolder.getTaskOrProvider())
    }
}