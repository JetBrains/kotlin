/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.newFileProperty

open class NodeJsExec : AbstractExecTask<NodeJsExec>(NodeJsExec::class.java), RequiresNpmDependencies {
    @get:Internal
    lateinit var nodeJs: NodeJsRootExtension

    @get:Internal
    override lateinit var compilation: KotlinJsCompilation

    init {
        onlyIf {
            !inputFileProperty.isPresent || inputFileProperty.asFile.map {
                it.exists()
            }.get()
        }
    }

    @Input
    var sourceMapStackTraces = true

    @InputFile
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @get:Internal
    override val nodeModulesRequired: Boolean
        get() = true

    @get:Internal
    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = mutableListOf<RequiredKotlinJsDependency>().also {
            if (sourceMapStackTraces) {
                it.add(nodeJs.versions.sourceMapSupport)
            }
        }

    override fun exec() {
        if (inputFileProperty.isPresent) {
            args(inputFileProperty.asFile.get())
        }

        if (sourceMapStackTraces) {
            val sourceMapSupportArgs = mutableListOf(
                "--require",
                compilation.npmProject.require("source-map-support/register.js")
            )

            args?.let { sourceMapSupportArgs.addAll(it) }

            args = sourceMapSupportArgs
        }

        super.exec()
    }

    companion object {
        fun create(
            compilation: KotlinJsCompilation,
            name: String,
            configuration: NodeJsExec.() -> Unit = {}
        ): TaskProvider<NodeJsExec> {
            val target = compilation.target
            val project = target.project
            val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

            return project.registerTask(name) {
                it.nodeJs = nodeJs
                it.compilation = compilation
                it.executable = nodeJs.requireConfigured().nodeExecutable
                it.dependsOn(nodeJs.npmInstallTask)

                val compileKotlinTask = compilation.compileKotlinTask
                it.dependsOn(nodeJs.npmInstallTask, compileKotlinTask)

                it.configuration()
            }
        }
    }
}