/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.npm

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.assertTrue

// TODO also test npm. Create `local.properties` file with `kotlin.js.yarn=false`.
class WasmNpmResolverPluginTest {

    private fun setupProject(
        configure: Project.() -> Unit = {},
    ): Project {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName("demo-project")
            }
        )

        val fileBasedNpmDep1 =
            project.projectDir.resolve("fileBasedNpmDep1").apply { mkdirs() }
        val fileBasedNpmDep2 =
            project.projectDir.resolve("nested/blah/fileBasedNpmDep2").apply { mkdirs() }

        project.configureRepositoriesForTests()

        project.kotlin {
            wasmWasi {
                nodejs()
                binaries.executable()
            }
            wasmJs {
                browser()
                binaries.executable()
            }
            js {
                browser()
                binaries.executable()
            }

            sourceSets.commonMain {
                dependencies {
                    implementation(npm("is-even", "1.0.0"))
                    implementation(npm(fileBasedNpmDep1))
                    implementation(npm(fileBasedNpmDep2))
                }
            }
        }

        project.runLifecycleAwareTest {
            configure(project)
            project.configurationResult.await()
        }

        return project
    }

    //region test expected RequiresNpmDependenciesTasks
    @Test
    fun `test RequiresNpmDependenciesTask for WasmWASI`() {
        val project = setupProject()

        testRequiresNpmDependenciesTasks(
            project.multiplatformExtension.wasmWasi(),
            listOf(
                "wasmWasiNodeDevelopmentRun",
                "wasmWasiNodeProductionRun",
                "wasmWasiNodeTest",
            )
        )
    }

    @Test
    fun `test RequiresNpmDependenciesTask for WasmJS`() {
        val project = setupProject()

        testRequiresNpmDependenciesTasks(
            project.multiplatformExtension.wasmJs(),
            listOf(
                "wasmJsBrowserDevelopmentRun",
                "wasmJsBrowserDevelopmentWebpack",
                "wasmJsBrowserProductionRun",
                "wasmJsBrowserProductionWebpack",
                "wasmJsBrowserTest",
            )
        )
    }

    @Test
    fun `test RequiresNpmDependenciesTask for JS`() {
        val project = setupProject()

        testRequiresNpmDependenciesTasks(
            project.multiplatformExtension.js(),
            listOf(
                "jsBrowserDevelopmentRun",
                "jsBrowserDevelopmentWebpack",
                "jsBrowserProductionRun",
                "jsBrowserProductionWebpack",
                "jsBrowserTest",
            )
        )
    }

    private fun testRequiresNpmDependenciesTasks(
        target: KotlinTarget,
        expectedTaskNames: Collection<String>,
    ) {
        val requiresNpmDependenciesTasks =
            target.project.getRequiresNpmDependenciesTasksFor(target)

        assertEquals(
            expectedTaskNames.prettyPrinted,
            requiresNpmDependenciesTasks.map { it.name }.prettyPrinted,
        )
    }
    //endregion

    //region test fileBasedNpmDependencyLocations
    @TestFactory
    fun `fileBasedNpmDependencyLocations JS`(): Stream<DynamicNode> {
        val project = setupProject()
        return testFileBasedNpmDependencyLocations(
            target = project.multiplatformExtension.js(),
            expectedFileBasedNpmDependencyLocations = listOf(
                "projectDir:fileBasedNpmDep1",
                "projectDir:nested/blah/fileBasedNpmDep2",
            ),
        )
    }

    @TestFactory
    fun `fileBasedNpmDependencyLocations WasmJS`(): Stream<DynamicNode> {
        val project = setupProject()
        return testFileBasedNpmDependencyLocations(
            target = project.multiplatformExtension.wasmJs(),
            expectedFileBasedNpmDependencyLocations = listOf(
                "projectDir:fileBasedNpmDep1",
                "projectDir:nested/blah/fileBasedNpmDep2",
            ),
        )
    }

    @TestFactory
    fun `fileBasedNpmDependencyLocations WasmWASI`(): Stream<DynamicNode> {
        val project = setupProject()
        return testFileBasedNpmDependencyLocations(
            target = project.multiplatformExtension.wasmWasi(),
            expectedFileBasedNpmDependencyLocations = emptyList(),
        )
    }

    private fun testFileBasedNpmDependencyLocations(
        target: KotlinTarget,
        expectedFileBasedNpmDependencyLocations: List<String>,
    ): Stream<DynamicNode> {
        return testEachRequiresNpmDependenciesTask(target, { task ->
            dynamicTest("file based npm dependency locations") {
                assertEquals(
                    expectedFileBasedNpmDependencyLocations.prettyPrinted,
                    task.normalizedFileBasedNpmDependencyLocations()?.prettyPrinted,
                    "Task ${task.path} has unexpected file-based npm dependencies."
                )
            }
        })
    }
    //endregion

    //region test lockfiles
    @TestFactory
    fun `lockfiles JS`(): Stream<DynamicNode> {
        val project = setupProject()
        return testLockFiles(
            target = project.multiplatformExtension.js(),
        ) { task ->
            when (task.name) {
                "jsBrowserTest" -> listOf(
                    "projectDir:build/js/yarn.lock",
                    "projectDir:build/js/packages/demo-project-test/yarn.lock",
                )
                else -> listOf(
                    "projectDir:build/js/yarn.lock",
                    "projectDir:build/js/packages/demo-project/yarn.lock",
                )
            }
        }
    }

    @TestFactory
    fun `lockfiles WasmJS`(): Stream<DynamicNode> {
        val project = setupProject()
        return testLockFiles(
            target = project.multiplatformExtension.wasmJs(),
        ) { _ ->
            listOf(
                "projectDir:build/wasm/yarn.lock",
                "npmToolingDir:yarn.lock",
            )
        }
    }

    @TestFactory
    fun `lockfiles WasmJS - with custom WasmJS npm tooling dir`(): Stream<DynamicNode> {
        val project = setupProject {
            val wasmJsNpmToolingDir = project.projectDir.resolve("customWasmJsNpmToolingDir").apply { mkdirs() }

            project.rootProject.plugins.withType<WasmNodeJsRootPlugin>().configureEach { _ ->
                project.rootProject.extensions.getByType(WasmNpmTooling::class.java).apply {
                    installationDir.fileValue(project.projectDir.resolve(wasmJsNpmToolingDir))
                }
            }
        }

        return testLockFiles(
            target = project.multiplatformExtension.wasmJs(),
        ) { _ ->
            listOf(
                "projectDir:build/wasm/yarn.lock",
                "projectDir:customWasmJsNpmToolingDir/yarn.lock",
            )
        }
    }

    @TestFactory
    fun `lockfiles WasmWASI`(): Stream<DynamicNode> {
        val project = setupProject()
        return testLockFiles(
            target = project.multiplatformExtension.wasmWasi(),
        ) { _ ->
            emptyList()
        }
    }

    private fun testLockFiles(
        target: KotlinTarget,
        expectedNpmDependenciesLockFiles: (task: RequiresNpmDependenciesTask) -> List<String>,
    ): Stream<DynamicNode> {
        return testEachRequiresNpmDependenciesTask(
            target = target,
            { task ->
                dynamicTest("npm dependencies lock files") {
                    val expectedNpmDependenciesLockFiles = expectedNpmDependenciesLockFiles(task)
                    assertNpmDependenciesLockFiles(
                        task,
                        expectedNpmDependenciesLockFiles,
                    )
                }
            },
            { task ->
                dynamicTest("lockfiles registered as task inputs") {
                    assertLockFilesAreRegisteredAsTaskInputs(task)
                }
            }
        )
    }

    private fun assertLockFilesAreRegisteredAsTaskInputs(task: RequiresNpmDependenciesTask) {
        val allTaskInputs = task.inputs.files

        val notRegisteredAsInputs = task.npmDependenciesLockFiles
            .filter { it !in allTaskInputs }
            .files
            .map { it.invariantSeparatorsPath }

        assertTrue(
            notRegisteredAsInputs.isEmpty(),
            """
                                |Task ${task.path} has npm dependencies lock files not registered as task inputs.
                                |notRegisteredAsInputs:$notRegisteredAsInputs
                                |allTaskInputs: $allTaskInputs
                                """.trimMargin()
        )
    }
    //endregion

    companion object {

        /**
         * Run a [DynamicTest] for each [RequiresNpmDependenciesTask] in the given [target].
         */
        private fun testEachRequiresNpmDependenciesTask(
            target: KotlinTarget,
            vararg tests: (task: RequiresNpmDependenciesTask) -> DynamicTest,
        ): Stream<DynamicNode> {
            val project = target.project

            val requiresNpmDependenciesTasks = buildList {
                project.getRequiresNpmDependenciesTasksFor(target).all {
                    add(it)
                }
            }

            return requiresNpmDependenciesTasks.asSequence().map { task ->
                dynamicContainer(
                    "task ${task.path}",
                    tests.map { test -> test(task) }
                )
            }.asStream()
        }


        private fun RequiresNpmDependenciesTask.normalizedFileBasedNpmDependencyLocations(): List<String>? =
            fileBasedNpmDependencyLocations.orNull
                ?.map { normalizeFilePath(this, File(it)) }

        private fun RequiresNpmDependenciesTask.normalizedNpmDependenciesLockFiles(): List<String> =
            npmDependenciesLockFiles.files.map { normalizeFilePath(this, it) }

        private fun normalizeFilePath(
            task: RequiresNpmDependenciesTask,
            file: File,
        ): String {
            val projectDir = task.project.projectDir
            val npmToolingDir = task.compilation.npmToolingDir().get().asFile

            return when {
                file.startsWith(projectDir) ->
                    "projectDir:${file.relativeTo(projectDir).invariantSeparatorsPath}"
                file.startsWith(npmToolingDir) ->
                    "npmToolingDir:${file.relativeTo(npmToolingDir).invariantSeparatorsPath}"
                else ->
                    error("Unexpected lockfile location: $file. Did not start with $projectDir or $npmToolingDir.")
            }
        }

        private fun assertNpmDependenciesLockFiles(
            task: RequiresNpmDependenciesTask,
            expectedLockFiles: List<String>,
        ) {
            val actualLockFiles =
                task.normalizedNpmDependenciesLockFiles()

            assertEquals(
                expectedLockFiles.prettyPrinted,
                actualLockFiles.prettyPrinted,
                "Task ${task.path} has unexpected npm dependencies lock files."
            )
        }

        private fun Project.getRequiresNpmDependenciesTasksFor(
            target: KotlinTarget,
        ): DomainObjectCollection<RequiresNpmDependenciesTask> {
            return tasks
                .withType<RequiresNpmDependenciesTask>()
                .matching { task ->
                    target.compilations.any { it == task.compilation }
                }
        }
    }
}
