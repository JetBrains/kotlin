/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

class KotlinNodeJs(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "node"),
    KotlinJsNodeDsl {

    override fun runTask(body: NodeJsExec.() -> Unit) {
        (project.tasks.getByName(runTaskName) as NodeJsExec).body()
    }

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useNodeJs { }
    }

    override fun configureRun(compilation: KotlinJsCompilation) {
        val project = target.project

        val runTaskHolder = project.createOrRegisterTask<NodeJsExec>(disambiguateCamelCased("run")) { runTask ->
            val compileKotlinTask = compilation.compileKotlinTask
            runTask.dependsOn(target.project.nodeJs.root.npmResolveTask, compileKotlinTask)

            val npmProject = compilation.npmProject
            runTask.args(compileKotlinTask.outputFile)
        }

        addSourceMapSupport(compilation, runTaskHolder)

        target.runTask.dependsOn(runTaskHolder.getTaskOrProvider())
    }

    private fun addSourceMapSupport(
        compilation: KotlinJsCompilation,
        runTaskHolder: TaskHolder<NodeJsExec>
    ) {
        compilation.dependencies {
            runtimeOnly(kotlin("test-nodejs-runner"))
        }

        target.project.nodeJs.root.npmResolveTask.doLast {
            runTaskHolder.configure { runTask ->
                runTask.args(
                    "--require",
                    compilation.npmProject.require("kotlin-test-nodejs-runner/kotlin-nodejs-source-map-support")
                )
            }
        }
    }
}