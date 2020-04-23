/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File

open class KotlinPackageJsonTask : DefaultTask() {
    private lateinit var nodeJs: NodeJsRootExtension
    private lateinit var compilation: KotlinJsCompilation

    private val compilationResolver
        get() = nodeJs.npmResolutionManager.requireConfiguringState()[project][compilation]

    private val producer: KotlinCompilationNpmResolver.PackageJsonProducer
        get() = compilationResolver.packageJsonProducer

    private fun findDependentTasks(): Collection<Any> =
        producer.internalDependencies.map { dependentResolver ->
            dependentResolver.npmProject.packageJsonTask
        } + producer.internalCompositeDependencies.map { dependency ->
            dependency.includedBuild.task(":$PACKAGE_JSON_UMBRELLA_TASK_NAME")
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
        fun create(compilation: KotlinJsCompilation): TaskProvider<KotlinPackageJsonTask> {
            val target = compilation.target
            val project = target.project
            val npmProject = compilation.npmProject
            val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

            val rootClean = project.rootProject.tasks.getByName(BasePlugin.CLEAN_TASK_NAME)
            val npmInstallTask = nodeJs.npmInstallTask
            val packageJsonTaskName = npmProject.packageJsonTaskName
            val packageJsonUmbrella = project.rootProject.tasks.named(PACKAGE_JSON_UMBRELLA_TASK_NAME)
            val packageJsonTask = project.registerTask<KotlinPackageJsonTask>(packageJsonTaskName) { task ->
                task.nodeJs = nodeJs
                task.compilation = compilation
                task.description = "Create package.json file for $compilation"
                task.group = NodeJsRootPlugin.TASKS_GROUP_NAME

                task.dependsOn(target.project.provider { task.findDependentTasks() })
                task.mustRunAfter(rootClean)
            }
            packageJsonUmbrella.configure { task ->
                task.inputs.file(packageJsonTask.map { it.packageJson })
            }

            nodeJs.rootPackageJsonTask.mustRunAfter(packageJsonTask)

            compilation.compileKotlinTask.dependsOn(
                npmInstallTask,
                packageJsonTask
            )

            return packageJsonTask
        }
    }
}