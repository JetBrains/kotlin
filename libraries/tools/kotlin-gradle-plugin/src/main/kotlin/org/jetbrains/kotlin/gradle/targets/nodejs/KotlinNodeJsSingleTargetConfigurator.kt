/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.nodejs

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilationTestsConfigurator
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

class KotlinNodeJsSingleTargetConfigurator(kotlinPluginVersion: String) :
    KotlinJsTargetConfigurator(kotlinPluginVersion) {

    override fun configureTarget(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        super.configureTarget(target)
        configureApplication(target)


    }

    private fun configureApplication(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target.compilations.all { compilation ->
            if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                // source maps support
                compilation.dependencies {
                    runtimeOnly(kotlin("test-nodejs-runner"))
                }

                val project = target.project
                project.createOrRegisterTask<NodeJsExec>("run") {
                    it.args(
                        project.npmProject.compileOutput(compilation.compileKotlinTask),
                        "--require", "kotlin-test-nodejs-runner/kotlin-nodejs-source-map-support.js"
                    )
                }
            }
        }
    }

    override fun newTestsConfigurator(compilation: KotlinJsCompilation) =
        object : KotlinJsCompilationTestsConfigurator(compilation) {
            override fun configureDefaultTestFramework(it: KotlinJsTest) {
                it.useNodeJs { }
            }
        }
}