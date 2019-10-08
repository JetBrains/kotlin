/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.plugins.ExtensionAware
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

abstract class KotlinJsSubTarget(
    val target: KotlinJsTarget,
    private val disambiguationClassifier: String
) : KotlinJsSubTargetDsl {
    val project get() = target.project
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

    val runTaskName = disambiguateCamelCased("run")
    val testTaskName = disambiguateCamelCased("test")

    final override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
        private set

    fun configure() {
        NpmResolverPlugin.apply(project)

        configureTests()
        configureRun()

        target.compilations.all {
            val npmProject = it.npmProject
            it.compileKotlinTask.kotlinOptions.outputFile = npmProject.dir.resolve(npmProject.main).canonicalPath
        }
    }

    protected fun disambiguateCamelCased(name: String): String =
        lowerCamelCaseName(target.disambiguationClassifier, disambiguationClassifier, name)

    private fun configureTests() {
        testRuns = project.container(KotlinJsPlatformTestRun::class.java) { name -> KotlinJsPlatformTestRun(name, this) }.also {
            (this as ExtensionAware).extensions.add(this::testRuns.name, it)
        }

        testRuns.all { configureTestRunDefaults(it) }
        testRuns.create(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
    }

    protected open fun configureTestRunDefaults(testRun: KotlinJsPlatformTestRun) {
        target.compilations.matching { it.name == KotlinCompilation.TEST_COMPILATION_NAME }.all { compilation ->
            configureTestsRun(testRun, compilation)
        }
    }

    abstract val testTaskDescription: String

    private fun configureTestsRun(testRun: KotlinJsPlatformTestRun, compilation: KotlinJsCompilation) {
        fun KotlinJsPlatformTestRun.subtargetTestTaskName(): String = disambiguateCamelCased(
            lowerCamelCaseName(
                name.takeIf { it != KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME },
                AbstractKotlinTargetConfigurator.testTaskNameSuffix
            )
        )

        val testJs = project.registerTask<KotlinJsTest>(testRun.subtargetTestTaskName()) { testJs ->
            val compileTask = compilation.compileKotlinTask

            testJs.group = LifecycleBasePlugin.VERIFICATION_GROUP
            testJs.description = testTaskDescription

            testJs.dependsOn(nodeJs.npmInstallTask, compileTask, nodeJs.nodeJsSetupTask)

            testJs.onlyIf {
                compileTask.outputFile.exists()
            }

            testJs.compilation = compilation
            testJs.targetName = listOfNotNull(target.disambiguationClassifier, disambiguationClassifier)
                .takeIf { it.isNotEmpty() }
                ?.joinToString()

            testJs.configureConventions()
        }

        testRun.executionTask = testJs

        target.testRuns.matching { it.name == testRun.name }.all { parentTestRun ->
            target.project.kotlinTestRegistry.registerTestTask(
                testJs,
                parentTestRun.executionTask
            )
        }

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
        testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).executionTask.configure(body)
    }
}