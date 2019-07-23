/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.TaskHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import java.io.File

open class KotlinPackageJsonTask : DefaultTask() {
    private lateinit var nodeJs: NodeJsRootExtension
    private lateinit var compilation: KotlinJsCompilation

    private val compilationResolver
        get() = nodeJs.npmResolutionManager.requireConfiguringState()[project][compilation]

    private val producer: KotlinCompilationNpmResolver.PackageJsonProducer
        get() = compilationResolver.packageJsonProducer

    private fun findDependentTasks(): Collection<KotlinPackageJsonTask> =
        producer.internalDependencies.map { dependentResolver ->
            dependentResolver.npmProject.packageJsonTask
        }

    @get:Nested
    internal val producerInputs: KotlinCompilationNpmResolver.PackageJsonProducerInputs
        get() = producer.inputs

    @get:OutputFile
    val packageJson: File
        get() = compilationResolver.npmProject.packageJsonFile

    @TaskAction
    fun resolve() {
        compilationResolver.resolve()
    }

    companion object {
        fun create(compilation: KotlinJsCompilation): TaskHolder<KotlinPackageJsonTask> {
            val target = compilation.target
            val project = target.project
            val npmProject = compilation.npmProject
            val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

            val rootClean = project.rootProject.tasks.getByName(BasePlugin.CLEAN_TASK_NAME)
            val npmInstallTask = nodeJs.npmInstallTask
            val packageJsonTaskName = npmProject.packageJsonTaskName
            val packageJsonTask = project.createOrRegisterTask<KotlinPackageJsonTask>(packageJsonTaskName) { task ->
                task.nodeJs = nodeJs
                task.compilation = compilation
                task.description = "Create package.json file for $compilation"
                task.group = NodeJsRootPlugin.TASKS_GROUP_NAME

                @Suppress("UnstableApiUsage")
                task.dependsOn(target.project.provider { task.findDependentTasks() })
                task.mustRunAfter(rootClean)
            }

            npmInstallTask.mustRunAfter(rootClean, packageJsonTask.getTaskOrProvider())

            compilation.compileKotlinTask.dependsOn(
                npmInstallTask,
                packageJsonTask.getTaskOrProvider()
            )

            return packageJsonTask
        }
    }
}