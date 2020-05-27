/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

abstract class KotlinJsIrSubTarget(
    val target: KotlinJsIrTarget,
    private val disambiguationClassifier: String
) : KotlinJsSubTargetDsl {
    val project get() = target.project

    init {
        val kotlinPluginInMultipleProjectsHolder = KotlinPluginInMultipleProjectsHolder(
            differentVersionsInDifferentProject = false
        )

        val anyProjectAffected = kotlinPluginInMultipleProjectsHolder.getAffectedProjects(project)
            ?.isNotEmpty() ?: false

        if (anyProjectAffected) {
            error(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)
        }

        kotlinPluginInMultipleProjectsHolder
            .addProject(project)
    }

    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

    abstract val testTaskDescription: String

    final override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
        private set

    protected val taskGroupName = "Kotlin $disambiguationClassifier"

    internal fun configure() {
        NpmResolverPlugin.apply(project)

        configureTests()

        target.compilations.all {
            val npmProject = it.npmProject
            it.binaries
                .withType(JsIrBinary::class.java)
                .all { binary ->
                    binary.linkTask.configure {
                        it.kotlinOptions.outputFile = npmProject.dir.resolve(npmProject.main).canonicalPath
                    }
                }
        }
    }

    private val produceExecutable: Unit by lazy {
        configureMain()
    }

    internal fun produceExecutable() {
        produceExecutable
    }

    override fun testTask(body: KotlinJsTest.() -> Unit) {
        testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).executionTask.configure(body)
    }

    protected fun disambiguateCamelCased(vararg names: String): String =
        lowerCamelCaseName(target.disambiguationClassifier, disambiguationClassifier, *names)

    private fun configureTests() {
        testRuns = project.container(KotlinJsPlatformTestRun::class.java) { name -> KotlinJsPlatformTestRun(name, target) }.also {
            (this as ExtensionAware).extensions.add(this::testRuns.name, it)
        }

        testRuns.all { configureTestRunDefaults(it) }
        testRuns.create(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
    }

    protected open fun configureTestRunDefaults(testRun: KotlinJsPlatformTestRun) {
        target.compilations.matching { it.name == KotlinCompilation.TEST_COMPILATION_NAME }
            .all { compilation ->
                configureTestsRun(testRun, compilation)
            }
    }

    private fun configureTestsRun(testRun: KotlinJsPlatformTestRun, compilation: KotlinJsIrCompilation) {
        fun KotlinJsPlatformTestRun.subtargetTestTaskName(): String = disambiguateCamelCased(
            lowerCamelCaseName(
                name.takeIf { it != KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME },
                AbstractKotlinTargetConfigurator.testTaskNameSuffix
            )
        )

        val testJs = project.registerTask<KotlinJsTest>(testRun.subtargetTestTaskName()) { testJs ->
            testJs.group = LifecycleBasePlugin.VERIFICATION_GROUP
            testJs.description = testTaskDescription

            val testExecutableTask = compilation.binaries.getIrBinaries(
                KotlinJsBinaryType.DEVELOPMENT
            ).single().linkTask

            testJs.inputFileProperty.set(
                testExecutableTask.map { it.outputFileProperty.get() }
            )

            testJs.dependsOn(nodeJs.npmInstallTask, nodeJs.nodeJsSetupTask)

            testJs.onlyIf { task ->
                (task as KotlinJsTest).inputFileProperty
                    .asFile
                    .map { it.exists() }
                    .get()
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

    private fun configureMain() {
        target.compilations.all { compilation ->
            if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                configureMain(compilation)
            }
        }
    }

    private fun configureMain(compilation: KotlinJsIrCompilation) {
        configureRun(compilation)
        configureBuild(compilation)
    }

    protected abstract fun configureRun(compilation: KotlinJsIrCompilation)

    protected abstract fun configureBuild(compilation: KotlinJsIrCompilation)

    internal inline fun <reified T : Task> registerSubTargetTask(
        name: String,
        noinline body: (T) -> (Unit)
    ): TaskProvider<T> =
        project.registerTask(name) {
            it.group = taskGroupName
            body(it)
        }

    companion object {
        const val RUN_TASK_NAME = "run"
    }
}