/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

class KotlinNodeJs(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "nodeJs"),
    KotlinJsNodeDsl {

    override fun runTask(body: NodeJsExec.() -> Unit) {
        (project.tasks.getByName(runTaskName) as NodeJsExec).body()
    }

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useNodeJs { }
    }

    override fun configureRun(compilation: KotlinJsCompilation) {
        // source maps support
        compilation.dependencies {
            runtimeOnly(kotlin("test-nodejs-runner"))
        }

        val project = target.project
        project.createOrRegisterTask<NodeJsExec>(disambiguateCamelCased("run")) {
            val npmProject = project.npmProject
            it.args(npmProject.compileOutput(compilation.compileKotlinTask))

            // source maps support
            it.args("--require", npmProject.getModuleEntryPath("kotlin-test-nodejs-runner"))

            target.runTask.dependsOn(it)
        }
    }
}