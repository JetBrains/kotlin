/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JsSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinJsNodeModulesTask
import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinNodeJsTestRuntimeToNodeModulesTask
import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinNodeJsTestTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

class KotlinJsTargetConfigurator(kotlinPluginVersion: String) :
    KotlinTargetConfigurator<KotlinJsCompilation>(true, true, kotlinPluginVersion) {

    override fun buildCompilationProcessor(compilation: KotlinJsCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JsSourceSetProcessor(compilation.target.project, tasksProvider, compilation, kotlinPluginVersion)
    }

    override fun configureTest(target: KotlinOnlyTarget<KotlinJsCompilation>) {
        target.compilations.all {
            configureTest(it)
        }
    }

    private fun configureTest(compilation: KotlinCompilationToRunnableFiles<*>) {
        val target = compilation.target
        val project = target.project
        val name = compilation.name
        val compileTestKotlin2Js = compilation.compileKotlinTask as Kotlin2JsCompile
        val isDefaultTarget = compilation.name.isBlank()

        fun camelCaseName(prefix: String): String {
            return if (isDefaultTarget) prefix
            else name + prefix.capitalize()
        }

        fun underscoredName(prefix: String): String {
            return if (isDefaultTarget) prefix
            else "${name}_$prefix"
        }

        val nodeModulesDir = project.buildDir.resolve(underscoredName("node_modules"))
        val nodeModulesTask = project.tasks.create(
            camelCaseName("kotlinJsNodeModules"),
            KotlinJsNodeModulesTask::class.java
        ) {
            it.dependsOn(compileTestKotlin2Js)

            it.nodeModulesDir = nodeModulesDir
            it.compile = compileTestKotlin2Js
        }

        val nodeModulesTestRuntimeTask = project.tasks.create(
            camelCaseName("kotlinJsNodeModulesTestRuntime"),
            KotlinNodeJsTestRuntimeToNodeModulesTask::class.java
        ) {
            it.nodeModulesDir = nodeModulesDir
        }

        val testJs: KotlinNodeJsTestTask =
            if (isDefaultTarget) project.tasks.create("testJs", KotlinNodeJsTestTask::class.java)
            else project.tasks.replace(camelCaseName("test"), KotlinNodeJsTestTask::class.java)

        testJs.also {
            it.group = "verification"
            it.dependsOn(nodeModulesTask, nodeModulesTestRuntimeTask)

            it.nodeModulesDir = nodeModulesDir
            it.nodeModulesToLoad = setOf(compileTestKotlin2Js.outputFile.name)
        }

        project.afterEvaluate {
            project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME)?.dependsOn(testJs)
        }
    }
}