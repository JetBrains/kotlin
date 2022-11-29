/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.newFileProperty

abstract class KotlinJsIrSubTarget(
    val target: KotlinJsIrTarget,
    private val disambiguationClassifier: String
) : KotlinJsSubTargetDsl {
    val project get() = target.project

    abstract val testTaskDescription: String

    final override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
        private set

    protected val taskGroupName = "Kotlin $disambiguationClassifier"

    @ExperimentalDistributionDsl
    override fun distribution(body: Distribution.() -> Unit) {
        target.binaries
            .all {
                it.distribution.body()
            }
    }

    internal fun configure() {
        NpmResolverPlugin.apply(project)

        target.compilations.all {
            val npmProject = it.npmProject
            it.kotlinOptions {
                freeCompilerArgs += "$PER_MODULE_OUTPUT_NAME=${npmProject.name}"
            }
        }

        configureTests()
    }

    private val produceExecutable: Unit by lazy {
        configureMain()
    }

    internal fun produceExecutable() {
        produceExecutable
    }

    private val produceLibrary: Unit by lazy {
        configureLibrary()
    }

    internal fun produceLibrary() {
        produceLibrary
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

        val testJs = project.registerTask<KotlinJsTest>(
            testRun.subtargetTestTaskName(),
            listOf(compilation)
        ) { testJs ->
            testJs.group = LifecycleBasePlugin.VERIFICATION_GROUP
            testJs.description = testTaskDescription

            val binary = compilation.binaries.getIrBinaries(
                KotlinJsBinaryMode.DEVELOPMENT
            ).single()

            testJs.dependsOn(binary.linkSyncTask)
            testJs.inputFileProperty.fileProvider(
                binary.linkSyncTask.flatMap { linkSyncTask ->
                    binary.linkTask.flatMap { linkTask ->
                        linkTask.outputFileProperty.map {
                            linkSyncTask.destinationDir.resolve(it.name)
                        }
                    }
                }
            )

            configureTestDependencies(testJs)

            testJs.onlyIf { task ->
                (task as KotlinJsTest).inputFileProperty
                    .asFile
                    .map { it.exists() }
                    .get()
            }

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
                configureDefaultTestFramework(it)
            }
        }
    }

    protected abstract fun configureDefaultTestFramework(test: KotlinJsTest)
    protected abstract fun configureTestDependencies(test: KotlinJsTest)

    private fun configureMain() {
        target.compilations.all { compilation ->
            if (compilation.isMain()) {
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

    private fun configureLibrary() {
        target.compilations.all { compilation ->
            if (compilation.isMain()) {
                configureLibrary(compilation)
            }
        }
    }

    protected open fun configureLibrary(compilation: KotlinJsIrCompilation) {
        val project = compilation.target.project

        val assembleTaskProvider = project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)

        val npmProject = compilation.npmProject

        compilation.binaries
            .matching { it is Library }
            .all { binary ->
                binary as Library

                val mode = binary.mode

                val prepareJsLibrary = registerSubTargetTask<Copy>(
                    disambiguateCamelCased(
                        binary.name,
                        PREPARE_JS_LIBRARY_TASK_NAME
                    )
                ) {
                    it.from(project.tasks.named(npmProject.publicPackageJsonTaskName))
                    it.from(binary.linkSyncTask)

                    it.into(binary.distribution.directory)
                }

                val distributionTask = registerSubTargetTask<Task>(
                    disambiguateCamelCased(
                        binary.name,
                        DISTRIBUTION_TASK_NAME
                    )
                ) {
                    it.dependsOn(prepareJsLibrary)

                    it.outputs.dir(project.newFileProperty { binary.distribution.directory })
                }

                if (mode == KotlinJsBinaryMode.PRODUCTION) {
                    assembleTaskProvider.dependsOn(distributionTask)
                }
            }
    }

    internal inline fun <reified T : Task> registerSubTargetTask(
        name: String,
        args: List<Any> = emptyList(),
        noinline body: (T) -> (Unit)
    ): TaskProvider<T> =
        project.registerTask(name, args) {
            it.group = taskGroupName
            body(it)
        }

    companion object {
        const val RUN_TASK_NAME = "run"

        const val DISTRIBUTE_RESOURCES_TASK_NAME = "distributeResources"
        const val DISTRIBUTION_TASK_NAME = "distribution"

        const val PREPARE_JS_LIBRARY_TASK_NAME = "prepare"
    }
}
