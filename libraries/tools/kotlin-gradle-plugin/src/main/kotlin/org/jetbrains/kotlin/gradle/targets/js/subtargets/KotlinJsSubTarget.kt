/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.utils.addIfNotNull

abstract class KotlinJsSubTarget(
    val target: KotlinJsTarget,
    private val disambiguationClassifier: String
) : KotlinJsSubTargetDsl {
    val project get() = target.project

    val runTaskName = disambiguateCamelCased("run")
    val testTaskName = disambiguateCamelCased("test")

    fun configure() {
        configureTests()
        configureRun()

        target.compilations.all {
            val npmProject = it.npmProject
            it.compileKotlinTask.kotlinOptions.outputFile = npmProject.dir.resolve(npmProject.main).canonicalPath
        }
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

    abstract val testTaskDescription: String

    private fun configureTests(compilation: KotlinJsCompilation) {
        // apply plugin (cannot be done at task instantiation time)
        val nodeJs = NodeJsPlugin.apply(target.project).root

        val testJs = project.createOrRegisterTask<KotlinJsTest>(testTaskName) { testJs ->
            val compileTask = compilation.compileKotlinTask

            testJs.group = LifecycleBasePlugin.VERIFICATION_GROUP
            testJs.description = testTaskDescription

            testJs.dependsOn(target.project.nodeJs.root.npmResolveTask, compileTask, nodeJs.nodeJsSetupTask)

            testJs.onlyIf {
                compileTask.outputFile.exists()
            }

            testJs.compilation = compilation
            testJs.targetName = listOfNotNull(target.disambiguationClassifier, disambiguationClassifier)
                .takeIf { it.isNotEmpty() }
                ?.joinToString()

            testJs.configureConventions()
        }

        target.project.kotlinTestRegistry.registerTestTask(testJs, target.testTask.doGetTask())

        project.whenEvaluated {
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

    override fun testTask(body: KotlinJsTest.() -> Unit) {
        (project.tasks.getByName(testTaskName) as KotlinJsTest).body()
    }
}