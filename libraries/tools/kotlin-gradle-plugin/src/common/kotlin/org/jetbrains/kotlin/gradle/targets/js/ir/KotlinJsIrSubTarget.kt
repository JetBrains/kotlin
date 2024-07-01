/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.*
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile
import org.jetbrains.kotlin.gradle.utils.whenEvaluated

interface IKotlinJsIrSubTarget : KotlinJsSubTargetDsl, Named {
    fun processBinary()
}

abstract class KotlinJsIrSubTarget(
    val target: KotlinJsIrTarget,
    val disambiguationClassifier: String,
) : IKotlinJsIrSubTarget, KotlinJsSubTargetDsl {
    init {
        target.configureTestSideEffect
    }

    override fun getName(): String = disambiguationClassifier

    val project get() = target.project

    abstract val testTaskDescription: String

    final override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
        private set

    internal val taskGroupName = "Kotlin $disambiguationClassifier"

    val subTargetConfigurators: DomainObjectSet<SubTargetConfigurator<*, *>> =
        project.objects.domainObjectSet<SubTargetConfigurator<*, *>>()

    @ExperimentalDistributionDsl
    override fun distribution(body: Action<Distribution>) {
        target.binaries
            .all {
                body.execute(it.distribution)
            }
    }

    internal open fun configure() {
        target.compilations.all { compilation ->
            compilation.compileTaskProvider.configure { task ->
                task.compilerOptions {
                    freeCompilerArgs.add(compilation.outputModuleName.map { "$PER_MODULE_OUTPUT_NAME=$it" })
                }
            }
        }

        configureTests()
    }

    private val produceBinary: Unit by lazy {
        configureMainCompilation()
    }

    override fun processBinary() {
        produceBinary
    }

    override fun testTask(body: Action<KotlinJsTest>) {
        testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).executionTask.configure(body)
    }

    internal fun disambiguateCamelCased(vararg names: String?): String =
        lowerCamelCaseName(target.disambiguationClassifier, disambiguationClassifier, *names)

    private fun configureTests() {
        testRuns = project.container(KotlinJsPlatformTestRun::class.java) { name -> KotlinJsPlatformTestRun(name, target) }.also {
            (this as ExtensionAware).extensions.add(this::testRuns.name, it)
        }

        testRuns.all { configureTestRunDefaults(it) }
        testRuns.create(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
    }

    private fun configureTestRunDefaults(testRun: KotlinJsPlatformTestRun) {
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

            val inputFileProperty = if (target.wasmTargetType != KotlinWasmTargetType.WASI) {
                testJs.dependsOn(binary.linkSyncTask)
                binary.mainFileSyncPath
            } else {
                testJs.dependsOn(binary.linkTask)
                binary.mainFile
            }

            testJs.inputFileProperty.set(
                inputFileProperty
            )

            configureTestDependencies(testJs, binary)

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

    abstract fun configureDefaultTestFramework(test: KotlinJsTest)
    abstract fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary)
    abstract fun binaryInputFile(binary: JsIrBinary): Provider<RegularFile>
    abstract fun binarySyncTaskName(binary: JsIrBinary): String
    abstract fun binarySyncOutput(binary: JsIrBinary): Provider<Directory>

    private fun configureMainCompilation() {
        target.compilations.all { compilation ->
            if (compilation.isMain()) {
                configureCompilation(compilation)
            }
        }
    }

    open fun configureCompilation(compilation: KotlinJsIrCompilation) {
        setupRun(compilation)
        setupBuild(compilation)
    }

    open fun setupRun(compilation: KotlinJsIrCompilation) {
        subTargetConfigurators.configureEach {
            it.setupRun(compilation)
        }
    }

    open fun setupBuild(compilation: KotlinJsIrCompilation) {
        subTargetConfigurators.configureEach {
            it.setupBuild(compilation)
        }
    }

    companion object {
        const val RUN_TASK_NAME = "run"

        const val DISTRIBUTION_TASK_NAME = "distribution"
    }
}

internal inline fun <reified T : Task> KotlinJsIrSubTarget.registerSubTargetTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline body: (T) -> (Unit),
): TaskProvider<T> =
    project.registerTask(name, args) {
        it.group = taskGroupName
        body(it)
    }
