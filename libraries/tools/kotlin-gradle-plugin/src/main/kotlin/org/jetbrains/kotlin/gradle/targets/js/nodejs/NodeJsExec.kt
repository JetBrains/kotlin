/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

open class NodeJsExec : AbstractExecTask<NodeJsExec>(NodeJsExec::class.java), RequiresNpmDependencies {
    @get:Internal
    lateinit var nodeJs: NodeJsRootExtension

    @get:Internal
    override lateinit var compilation: KotlinJsCompilation

    @Input
    var sourceMapStackTraces = true

    @get:Internal
    override val nodeModulesRequired: Boolean
        get() = true

    @get:Internal
    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = mutableListOf<RequiredKotlinJsDependency>().also {
            if (sourceMapStackTraces) {
                it.add(nodeJs.versions.kotlinNodeJsTestRunner)
            }
        }

    override fun exec() {
        if (sourceMapStackTraces) {
            args(
                "--require",
                compilation.npmProject.require("kotlin-test-nodejs-runner/kotlin-nodejs-source-map-support")
            )
        }

        super.exec()
    }

    companion object {
        fun create(
            compilation: KotlinJsCompilation,
            name: String,
            configuration: NodeJsExec.() -> Unit = {}
        ): TaskHolder<NodeJsExec> {
            val target = compilation.target
            val project = target.project
            val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

            return project.createOrRegisterTask(name) {
                it.nodeJs = nodeJs
                it.compilation = compilation
                it.executable = nodeJs.environment.nodeExecutable
                it.dependsOn(nodeJs.npmInstallTask)

                val compileKotlinTask = compilation.compileKotlinTask
                it.dependsOn(nodeJs.npmInstallTask, compileKotlinTask)
                it.args(compileKotlinTask.outputFile)

                it.configuration()
            }
        }
    }
}