/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fakePackageJsonValue
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File

open class KotlinPackageJsonTask : DefaultTask() {

    init {
        onlyIf {
            nodeJs.npmResolutionManager.isConfiguringState()
        }
    }

    private lateinit var nodeJs: NodeJsRootExtension

    @Transient
    private lateinit var compilation: KotlinJsCompilation

    private val compilationName by lazy {
        compilation.name
    }

    @Input
    val projectPath = project.path

    private val compilationResolver
        get() = nodeJs.npmResolutionManager.resolver[projectPath][compilation]

    private val producer: KotlinCompilationNpmResolver.PackageJsonProducer
        get() = compilationResolver.packageJsonProducer

    @get:Input
    val packageJsonCustomFields: Map<String, Any?> by lazy {
        PackageJson(fakePackageJsonValue, fakePackageJsonValue)
            .apply {
                compilation.packageJsonHandlers.forEach { it() }
            }.customFields
    }

    private fun findDependentTasks(): Collection<Any> =
        producer.internalDependencies.map { dependentResolver ->
            dependentResolver.npmProject.packageJsonTask
        } + producer.internalCompositeDependencies.map { dependency ->
            dependency.includedBuild.task(":$PACKAGE_JSON_UMBRELLA_TASK_NAME")
        }

    @get:Input
    internal val toolsNpmDependencies: List<String> by lazy {
        nodeJs.taskRequirements
            .getCompilationNpmRequirements(compilationName)
            .map { it.toString() }
    }

    @get:Nested
    internal val producerInputs: KotlinCompilationNpmResolver.PackageJsonProducerInputs by lazy {
        producer.inputs
    }

    @get:OutputFile
    val packageJson: File by lazy {
        compilationResolver.npmProject.prePackageJsonFile
    }

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

            val rootClean = project.rootProject.tasks.named(BasePlugin.CLEAN_TASK_NAME)
            val npmInstallTask = nodeJs.npmInstallTaskProvider
            val packageJsonTaskName = npmProject.packageJsonTaskName
            val packageJsonUmbrella = nodeJs.packageJsonUmbrellaTaskProvider
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

            nodeJs.rootPackageJsonTaskProvider?.configure { it.mustRunAfter(packageJsonTask) }

            compilation.compileKotlinTaskProvider.dependsOn(npmInstallTask)
            compilation.compileKotlinTaskProvider.dependsOn(packageJsonTask)

            return packageJsonTask
        }
    }
}