/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

class KotlinNodeJs(target: KotlinOnlyTarget<KotlinJsCompilation>) :
    KotlinJsInnerTargetConfigurator(target) {

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useNodeJs { }
    }

    override fun configureRun(compilation: KotlinJsCompilation) {
        // source maps support
        compilation.dependencies {
            runtimeOnly(kotlin("test-nodejs-runner"))
        }

        val project = target.project
        project.createOrRegisterTask<NodeJsExec>("run") {
            it.args(project.npmProject.compileOutput(compilation.compileKotlinTask))

            // source maps support
            it.args("--require", "kotlin-test-nodejs-runner/kotlin-nodejs-source-map-support.js")
        }
    }
}