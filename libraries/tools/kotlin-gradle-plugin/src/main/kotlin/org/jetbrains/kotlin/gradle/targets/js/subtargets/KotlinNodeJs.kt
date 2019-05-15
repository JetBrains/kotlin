/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
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
            runTask.dependsOn(target.npmResolveTaskHolder.getTaskOrProvider(), compileKotlinTask)

            val npmProject = project.npmProject
            runTask.args(npmProject.compileOutput(compileKotlinTask))
        }

        addSourceMapSupport(compilation, runTaskHolder, project)

        target.runTask.dependsOn(runTaskHolder.getTaskOrProvider())
    }

    private fun addSourceMapSupport(
        compilation: KotlinJsCompilation,
        runTaskHolder: TaskHolder<NodeJsExec>,
        project: Project
    ) {
        compilation.dependencies {
            runtimeOnly(kotlin("test-nodejs-runner"))
        }

        target.npmResolveTaskHolder.doGetTask().doLast {
            runTaskHolder.configure { runTask ->
                runTask.args(
                    "--require",
                    project.npmProject.require("kotlin-test-nodejs-runner/kotlin-nodejs-source-map-support")
                )
            }
        }
    }
}