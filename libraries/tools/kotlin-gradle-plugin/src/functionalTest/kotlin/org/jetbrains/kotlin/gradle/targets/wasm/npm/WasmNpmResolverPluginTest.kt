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
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
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
        val project = buildProjectWithMPP()

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

    @TestFactory
    fun `lockfiles JS`(): Stream<DynamicNode> {
        val project = setupProject()
        return assertRequiresNpmDependenciesTasksHaveLockFiles(
            target = project.multiplatformExtension.js(),
            expectedFileBasedNpmDependencyLocations = listOf(
                "projectDir:fileBasedNpmDep1",
                "projectDir:nested/blah/fileBasedNpmDep2",
            ),
            expectedNpmDependenciesLockFiles = listOf(
                "projectDir:build/js/yarn.lock",
                "npmToolingDir:yarn.lock",
            ),
        )
    }

    @TestFactory
    fun `lockfiles WasmJS`(): Stream<DynamicNode> {
        val project = setupProject()
        return assertRequiresNpmDependenciesTasksHaveLockFiles(
            target = project.multiplatformExtension.wasmJs(),
            expectedFileBasedNpmDependencyLocations = listOf(
                "projectDir:fileBasedNpmDep1",
                "projectDir:nested/blah/fileBasedNpmDep2",
            ),
            expectedNpmDependenciesLockFiles = listOf(
                "projectDir:build/wasm/yarn.lock",
                "npmToolingDir:yarn.lock",
            ),
        )
    }

    @TestFactory
    fun `lockfiles WasmJS - custom WasmJS npm tooling dir`(): Stream<DynamicNode> {
        val project = setupProject {
            val wasmJsNpmToolingDir = project.projectDir.resolve("customWasmJsNpmToolingDir").apply { mkdirs() }
//            project.plugins.withType<WasmNodeJsRootPlugin>().configureEach { _ ->
//            }
            project.extensions.getByType(WasmNpmTooling::class.java).apply {
                installationDir.set(wasmJsNpmToolingDir)
            }
        }

//        val wasmJsNpmToolingDir = project.projectDir.resolve("customWasmJsNpmToolingDir").apply { mkdirs() }

//        project.plugins.withType<WasmNodeJsRootPlugin>().configureEach { _ ->
//            project.extensions.getByType(WasmNpmTooling::class.java).apply {
//                installationDir.fileValue(wasmJsNpmToolingDir)
//            }
//        }

        return assertRequiresNpmDependenciesTasksHaveLockFiles(
            target = project.multiplatformExtension.wasmJs(),
            expectedFileBasedNpmDependencyLocations = listOf(
                "projectDir:fileBasedNpmDep1",
                "projectDir:nested/blah/fileBasedNpmDep2",
            ),
            expectedNpmDependenciesLockFiles = listOf(
                "projectDir:build/wasm/yarn.lock",
                "projectDir:customWasmJsNpmToolingDir/yarn.lock",
            ),
        )
    }

    @TestFactory
    fun `lockfiles WasmWASI`(): Stream<DynamicNode> {
        val project = setupProject()
        return assertRequiresNpmDependenciesTasksHaveLockFiles(
            target = project.multiplatformExtension.wasmWasi(),
            expectedFileBasedNpmDependencyLocations = emptyList(),
            expectedNpmDependenciesLockFiles = emptyList(),
        )
    }

    private fun assertRequiresNpmDependenciesTasksHaveLockFiles(
        target: KotlinTarget,
        expectedFileBasedNpmDependencyLocations: List<String>,
        expectedNpmDependenciesLockFiles: List<String>,
    ): Stream<DynamicNode> {
        val project = target.project

        val requiresNpmDependenciesTasks =
            project.getRequiresNpmDependenciesTasksFor(target)

        val tasks2 = buildList {
            requiresNpmDependenciesTasks.all {
                add(it)
            }
        }

        return sequence<DynamicNode> {
            tasks2.forEach { task ->
                yield(
                    DynamicContainer.dynamicContainer("task ${task.path}", sequence<DynamicNode> {
                        yield(dynamicTest("file based npm dependency locations") {
                            assertEquals(
                                expectedFileBasedNpmDependencyLocations.prettyPrinted,
                                task.normalizedFileBasedNpmDependencyLocations()?.prettyPrinted,
                                "Task ${task.path} has unexpected file-based npm dependencies."
                            )
                        })

                        yield(dynamicTest("npm dependencies lock files") {
                            assertNpmDependenciesLockFiles(expectedNpmDependenciesLockFiles, task)
                        })

                        yield(dynamicTest("lockfiles registered as task inputs") {
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
                        })
                    }.asStream())
                )

            }
        }.asStream()
    }

    companion object {

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
                file.startsWith(npmToolingDir) ->
                    "npmToolingDir:${file.relativeTo(npmToolingDir).invariantSeparatorsPath}"
                file.startsWith(projectDir) ->
                    "projectDir:${file.relativeTo(projectDir).invariantSeparatorsPath}"
                else ->
                    error("Unexpected lockfile location: $file. Did not start with $projectDir or $npmToolingDir.")
            }
        }

        private fun assertNpmDependenciesLockFiles(
            expectedLockFiles: List<String>,
            task: RequiresNpmDependenciesTask,
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
