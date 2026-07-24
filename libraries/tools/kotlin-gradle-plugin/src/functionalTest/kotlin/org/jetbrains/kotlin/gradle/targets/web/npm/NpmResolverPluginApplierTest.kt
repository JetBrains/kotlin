/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
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

/**
 * Test [NpmResolverPluginApplier].
 */
sealed class NpmResolverPluginApplierTest(
    private val packageManager: String,
) {

    class Npm : NpmResolverPluginApplierTest(packageManager = "npm")

    class Yarn : NpmResolverPluginApplierTest(packageManager = "yarn")

    private val lockFileName: String =
        when (packageManager) {
            "npm" -> "package-lock.json"
            "yarn" -> "yarn.lock"
            else -> error("Unknown package manager: $packageManager")
        }

    /**
     * Create a KMP project with JS, WasmJS, and WasmWASI targets.
     *
     * Add npm dependencies (both regular and file-based) to `commonMain`.
     */
    private fun setupProject(
        configure: Project.() -> Unit = {},
    ): Project {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName("demo-project")
            },
            preApplyCode = {
                when (packageManager) {
                    "npm" -> {
                        projectDir.resolve("local.properties").appendText("\nkotlin.js.yarn=false\n")
                    }
                    "yarn" -> {
                        // yarn is the default - no need to configure
                    }
                    else -> error("Unknown package manager: $packageManager")
                }
            },
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

        configure(project)
        project.evaluate()
        return project
    }

    //region test expected RequiresNpmDependenciesTasks
    // Basic verification tests to ensure the tests below run against the correct expected tasks for each target.
    @Test
    fun `verify expected RequiresNpmDependenciesTasks for WasmWASI`() {
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
    fun `verify expected RequiresNpmDependenciesTasks for WasmJS`() {
        val project = setupProject()

        testRequiresNpmDependenciesTasks(
            project.multiplatformExtension.wasmJs(),
            listOf(
                "prepareWebpackBundleForKotlinJsTests",
                "wasmJsBrowserDevelopmentRun",
                "wasmJsBrowserDevelopmentWebpack",
                "wasmJsBrowserProductionRun",
                "wasmJsBrowserProductionWebpack",
                "wasmJsBrowserTest",
            )
        )
    }

    @Test
    fun `verify expected RequiresNpmDependenciesTasks for JS`() {
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
    fun `verify expected fileBasedNpmDependencyLocations for JS tasks`(): Stream<DynamicNode> {
        val project = setupProject()
        return testFileBasedNpmDependencyLocations(
            target = project.multiplatformExtension.js(),
            expectedFileBasedNpmDependencyLocations = listOf(
                "rootProjectDir:fileBasedNpmDep1",
                "rootProjectDir:nested/blah/fileBasedNpmDep2",
            ),
        )
    }

    @TestFactory
    fun `verify expected fileBasedNpmDependencyLocations for WasmJS tasks`(): Stream<DynamicNode> {
        val project = setupProject()
        return testFileBasedNpmDependencyLocations(
            target = project.multiplatformExtension.wasmJs(),
            expectedFileBasedNpmDependencyLocations = listOf(
                "rootProjectDir:fileBasedNpmDep1",
                "rootProjectDir:nested/blah/fileBasedNpmDep2",
            ),
        )
    }

    @TestFactory
    fun `verify expected fileBasedNpmDependencyLocations for WasmWASI tasks`(): Stream<DynamicNode> {
        val project = setupProject()
        // expect no dependencies because WASI does not have npm dependencies
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
                    "rootProjectDir:build/js/$lockFileName",
                )
                else -> listOf(
                    "rootProjectDir:build/js/$lockFileName",
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
                "rootProjectDir:build/wasm/$lockFileName",
                "npmToolingDir:$lockFileName",
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
                "rootProjectDir:build/wasm/$lockFileName",
                "rootProjectDir:customWasmJsNpmToolingDir/$lockFileName",
            )
        }
    }

    @TestFactory
    fun `lockfiles WasmWASI`(): Stream<DynamicNode> {
        val project = setupProject()
        // expect no lock files because WASI does not have npm dependencies
        return testLockFiles(
            target = project.multiplatformExtension.wasmWasi(),
        ) { _ ->
            emptyList()
        }
    }

    /**
     * Check each [org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask] for the given [target]
     * has registered [expectedNpmDependenciesLockFiles] as task inputs.
     */
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

        /**
         * Normalize the file paths for readability.
         */
        private fun normalizeFilePath(
            task: RequiresNpmDependenciesTask,
            file: File,
        ): String {
            val projectDir = task.project.projectDir
            val npmToolingDir = task.compilation.npmToolingDir().get().asFile

            return when {
                file.startsWith(projectDir) ->
                    "rootProjectDir:${file.relativeTo(projectDir).invariantSeparatorsPath}"
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
