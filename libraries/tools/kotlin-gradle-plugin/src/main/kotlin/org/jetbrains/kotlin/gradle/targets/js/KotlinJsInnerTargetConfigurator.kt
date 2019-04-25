/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.registerTestTask
import org.jetbrains.kotlin.utils.addIfNotNull

abstract class KotlinJsInnerTargetConfigurator(val target: KotlinOnlyTarget<KotlinJsCompilation>) {
    val project get() = target.project
    private val disambiguationClassifier get() = "browser"

    fun configure() {
        configureTests()
        configureRun()
    }

    private fun disambiguate(name: String): MutableList<String> {
        val components = mutableListOf<String>()

        components.addIfNotNull(target.disambiguationClassifier)
        components.add(disambiguationClassifier)
        components.add(name)
        return components
    }

    protected fun disambiguateCamelCased(name: String): String {
        val components = disambiguate(name)

        return components.first() + components.drop(1).joinToString("") { it.capitalize() }
    }

    private fun configureTests() {
        target.compilations.all { compilation ->
            if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
                configureTests(compilation)
            }
        }
    }

    private fun configureTests(compilation: KotlinJsCompilation) {
        val compileTask = compilation.compileKotlinTask
        val testTaskName = disambiguateCamelCased("test")

        // apply plugin (cannot be done at task instantiation time)
        val nodeJs = NodeJsPlugin.apply(target.project).root

        val testJs = project.createOrRegisterTask<KotlinJsTest>(testTaskName) { testJs ->
            testJs.group = LifecycleBasePlugin.VERIFICATION_GROUP

            testJs.dependsOn(compileTask, nodeJs.nodeJsSetupTask)

            testJs.onlyIf {
                compileTask.outputFile.exists()
            }

            testJs.runtimeDependencyHandler = compilation
            testJs.targetName = disambiguationClassifier
            testJs.nodeModulesToLoad.add(compileTask.outputFile.name)

            testJs.configureConventions()

            project.tasks.maybeCreate("test").dependsOn(testJs)
        }

        registerTestTask(testJs)

        project.afterEvaluate {
            testJs.configure {
                if (it.testFramework == null) {
                    configureDefaultTestFramework(it)
                }
            }
        }
    }

    protected abstract fun configureDefaultTestFramework(it: KotlinJsTest)
    fun configureRun() {
        target.compilations.all { compilation ->
            if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                configureRun(compilation)
            }
        }
    }

    protected abstract fun configureRun(compilation: KotlinJsCompilation)
}