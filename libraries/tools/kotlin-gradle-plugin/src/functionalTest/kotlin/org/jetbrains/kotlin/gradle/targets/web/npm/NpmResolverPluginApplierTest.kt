/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNpmTooling
import org.jetbrains.kotlin.gradle.tasks.withType
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
     * How npm dependencies are declared in a source set's `dependencies {}` block.
     */
    enum class NpmDependencyStyle {
        /** Legacy declarations added to a Configuration, e.g. `implementation(npm(...))`. */
        LegacyConfigurations,

        /** Declarations using the npm DSL, e.g. `npm(...)`. */
        KgpDslCollector,
    }

    /**
     * Create a KMP project with JS, WasmJS, and WasmWASI targets.
     *
     * Add npm dependencies (both regular and file-based) to `commonMain`,
     * declared using [npmDependencyStyle].
     */
    @Suppress("DEPRECATION") // the deprecated npm(directory) declaration is covered on purpose
    private fun setupProject(
        npmDependencyStyle: NpmDependencyStyle = NpmDependencyStyle.LegacyConfigurations,
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
                wasmtime()
                binaries.executable()
            }
            wasmJs {
                browser {
                    test.chromium()
                }
                binaries.executable()
            }
            js {
                browser {
                    test.chromium()
                }
                binaries.executable()
            }

            sourceSets.commonMain {
                dependencies {
                    when (npmDependencyStyle) {
                        NpmDependencyStyle.LegacyConfigurations -> {
                            implementation(npm("is-even", "1.0.0"))
                            implementation(npm(fileBasedNpmDep1))
                            implementation(npm(fileBasedNpmDep2))
                        }
                        NpmDependencyStyle.KgpDslCollector -> {
                            npm("is-even", "1.0.0")
                            npm("fileBasedNpmDep1", fileBasedNpmDep1)
                            npm("fileBasedNpmDep2", fileBasedNpmDep2)
                        }
                    }
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
    fun `verify expected RequiresNpmDependenciesTasks for WasmJS`() {
        val project = setupProject()

        testRequiresNpmDependenciesTasks(
            project.multiplatformExtension.wasmJs(),
            listOf(
                "kotlinInstallPlaywrightChromium",
                "wasmJsBrowserDevelopmentRun",
                "wasmJsBrowserDevelopmentWebpack",
                "wasmJsBrowserProductionRun",
                "wasmJsBrowserProductionWebpack",
                "wasmJsBrowserTest",
                "wasmJsTestBundleAsEsm",
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
                "prepareWebpackBundleForKotlinJsTests",
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
    fun `verify expected fileBasedNpmDependencyLocations for JS tasks`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle)
            testFileBasedNpmDependencyLocations(
                target = project.multiplatformExtension.js(),
                expectedFileBasedNpmDependencyLocations =
                    npmDependencyStyle.expectedFileBasedNpmDependencyLocations,
            )
        }

    @TestFactory
    fun `verify expected fileBasedNpmDependencyLocations for WasmJS tasks`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle)
            testFileBasedNpmDependencyLocations(
                target = project.multiplatformExtension.wasmJs(),
                expectedFileBasedNpmDependencyLocations =
                    npmDependencyStyle.expectedFileBasedNpmDependencyLocations,
            )
        }

    @TestFactory
    fun `verify expected fileBasedNpmDependencyLocations for WasmWASI tasks`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle)
            // expect no dependencies because WASI does not have npm dependencies
            testFileBasedNpmDependencyLocations(
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

    //region test fileBasedDependencyDirectories
    @TestFactory
    fun `verify expected fileBasedDependencyDirectories for JS tasks`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle)
            testFileBasedDependencyDirectories(
                target = project.multiplatformExtension.js(),
                expectedFileBasedDependencyDirectories = defaultFileBasedNpmDeps,
            )
        }

    @TestFactory
    fun `verify expected fileBasedDependencyDirectories for WasmJS tasks`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle)
            testFileBasedDependencyDirectories(
                target = project.multiplatformExtension.wasmJs(),
                expectedFileBasedDependencyDirectories = defaultFileBasedNpmDeps,
            )
        }

    @TestFactory
    fun `verify expected fileBasedDependencyDirectories for WasmWASI tasks`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle)
            // expect no directories because WASI does not have npm dependencies
            testFileBasedDependencyDirectories(
                target = project.multiplatformExtension.wasmWasi(),
                expectedFileBasedDependencyDirectories = emptyList(),
            )
        }

    @TestFactory
    fun `verify expected fileBasedDependencyDirectories for JS tasks - all npm scopes`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle) {
                val devDir = layout.projectDirectory.dir("devDep").apply { asFile.mkdirs() }
                val optionalDir = layout.projectDirectory.dir("optionalDep").apply { asFile.mkdirs() }
                val devDirectoryDir = layout.projectDirectory.dir("devDirectoryDep").apply { asFile.mkdirs() }
                val optionalDirectoryDir = layout.projectDirectory.dir("optionalDirectoryDep").apply { asFile.mkdirs() }

                // set after the declaration, to verify the directory is read lazily
                val lateDir = objects.directoryProperty()

                kotlin {
                    sourceSets.commonMain {
                        dependencies {
                            npmDev("devDep", devDir.asFile)
                            npmOptional("optionalDep", optionalDir.asFile)
                            npmDevDirectory("devDirectoryDep", provider { devDirectoryDir })
                            npmOptionalDirectory("optionalDirectoryDep", provider { optionalDirectoryDir })
                            npmDevDirectory("lateDep", lateDir)
                        }
                    }
                }

                lateDir.set(layout.projectDirectory.dir("lateDep").apply { asFile.mkdirs() })
            }

            testFileBasedDependencyDirectories(
                target = project.multiplatformExtension.js(),
                expectedFileBasedDependencyDirectories = listOf(
                    "rootProjectDir:devDep",
                    "rootProjectDir:devDirectoryDep",
                    "rootProjectDir:fileBasedNpmDep1",
                    "rootProjectDir:lateDep",
                    "rootProjectDir:nested/blah/fileBasedNpmDep2",
                    "rootProjectDir:optionalDep",
                    "rootProjectDir:optionalDirectoryDep",
                ),
            )
        }

    @TestFactory
    fun `verify expected fileBasedDependencyDirectories - target-specific source set`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle) {
                val jsOnlyDir = layout.projectDirectory.dir("jsOnlyDep").apply { asFile.mkdirs() }

                kotlin {
                    sourceSets.jsMain {
                        dependencies {
                            npm("jsOnlyDep", jsOnlyDir.asFile)
                        }
                    }
                }
            }

            Stream.concat(
                // JS tasks, including jsBrowserTest via the associated main compilation, track the jsMain directory
                testFileBasedDependencyDirectories(
                    target = project.multiplatformExtension.js(),
                    expectedFileBasedDependencyDirectories = listOf(
                        "rootProjectDir:fileBasedNpmDep1",
                        "rootProjectDir:jsOnlyDep",
                        "rootProjectDir:nested/blah/fileBasedNpmDep2",
                    ),
                ),
                // WasmJS tasks do not see the jsMain directory
                testFileBasedDependencyDirectories(
                    target = project.multiplatformExtension.wasmJs(),
                    expectedFileBasedDependencyDirectories = defaultFileBasedNpmDeps,
                ),
            )
        }

    /**
     * Records the current behaviour of an npm dependency declared using the npm DSL with a `file:` version string.
     *
     * The directory is neither tracked as a task input, nor does it disable caching,
     * so changes to the directory contents do not invalidate the task.
     * If this behaviour changes, update this test.
     */
    @TestFactory
    fun `verify file version string npm dependency is not tracked`(): Stream<DynamicNode> =
        forEachNpmDependencyStyle { npmDependencyStyle ->
            val project = setupProject(npmDependencyStyle = npmDependencyStyle) {
                val fileVersionDir = layout.projectDirectory.dir("fileVersionDep").apply { asFile.mkdirs() }

                kotlin {
                    sourceSets.commonMain {
                        dependencies {
                            npm("fileVersionDep", "file:${fileVersionDir.asFile.absolutePath}")
                        }
                    }
                }
            }

            Stream.concat(
                // test fileVersionDep is not tracked
                testFileBasedDependencyDirectories(
                    target = project.multiplatformExtension.js(),
                    expectedFileBasedDependencyDirectories = defaultFileBasedNpmDeps,
                ),
                // test fileVersionDep does not disable caching
                testFileBasedNpmDependencyLocations(
                    target = project.multiplatformExtension.js(),
                    expectedFileBasedNpmDependencyLocations = npmDependencyStyle.expectedFileBasedNpmDependencyLocations,
                ),
            )
        }

    /**
     * Run [tests] for each [NpmDependencyStyle].
     *
     * See [setupProject].
     */
    private fun forEachNpmDependencyStyle(
        tests: (npmDependencyStyle: NpmDependencyStyle) -> Stream<DynamicNode>,
    ): Stream<DynamicNode> =
        NpmDependencyStyle.entries.asSequence().map { npmDependencyStyle ->
            dynamicContainer(
                "npmDependencyStyle=$npmDependencyStyle",
                tests(npmDependencyStyle),
            )
        }.asStream()

    /**
     * The expected [RequiresNpmDependenciesTask.fileBasedNpmDependencyLocations] for projects created by [setupProject].
     *
     * Only [NpmDependencyStyle.LegacyConfigurations] declarations disable caching.
     * [NpmDependencyStyle.KgpDslCollector] declarations are tracked as task inputs instead.
     */
    private val NpmDependencyStyle.expectedFileBasedNpmDependencyLocations: List<String>
        get() = when (this) {
            NpmDependencyStyle.LegacyConfigurations -> defaultFileBasedNpmDeps
            NpmDependencyStyle.KgpDslCollector -> emptyList()
        }

    /**
     * Check each [RequiresNpmDependenciesTask] for the given [target]
     * tracks the [expectedFileBasedDependencyDirectories], and registers them as task inputs.
     */
    private fun testFileBasedDependencyDirectories(
        target: KotlinTarget,
        expectedFileBasedDependencyDirectories: List<String>,
    ): Stream<DynamicNode> {
        return testEachRequiresNpmDependenciesTask(
            target,
            { task ->
                dynamicTest("file based dependency directories") {
                    assertEquals(
                        expectedFileBasedDependencyDirectories.prettyPrinted,
                        task.normalizedFileBasedDependencyDirectories().prettyPrinted,
                        "Task ${task.path} has unexpected file-based dependency directories."
                    )
                }
            },
            { task ->
                dynamicTest("file based dependency directories registered as task inputs") {
                    val allTaskInputs = task.inputs.files

                    val notRegisteredAsInputs = task.fileBasedDependencyDirectories
                        .filter { it !in allTaskInputs }
                        .files
                        .map { it.invariantSeparatorsPath }

                    assertTrue(
                        notRegisteredAsInputs.isEmpty(),
                        """
                        |Task ${task.path} has file-based dependency directories not registered as task inputs.
                        |notRegisteredAsInputs:$notRegisteredAsInputs
                        |allTaskInputs: $allTaskInputs
                        """.trimMargin()
                    )
                }
            },
        )
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

            project.rootProject.plugins.withType(WasmNodeJsRootPlugin::class.java).configureEach { _ ->
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
     * has the [expectedNpmDependenciesLockFiles].
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
        )
    }
    //endregion

    companion object {

        /**
         * The file-based npm dependencies added by [setupProject].
         */
        private val defaultFileBasedNpmDeps: List<String> = listOf(
            "rootProjectDir:fileBasedNpmDep1",
            "rootProjectDir:nested/blah/fileBasedNpmDep2",
        )

        /**
         * Run a [DynamicTest] for each [RequiresNpmDependenciesTask] in the given [target].
         */
        private fun testEachRequiresNpmDependenciesTask(
            target: KotlinTarget,
            vararg tests: (task: RequiresNpmDependenciesTask) -> DynamicTest,
        ): Stream<DynamicNode> {
            val project = target.project

            val requiresNpmDependenciesTasks = project.getRequiresNpmDependenciesTasksFor(target)
                .toList()

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

        private fun RequiresNpmDependenciesTask.normalizedFileBasedDependencyDirectories(): List<String> =
            fileBasedDependencyDirectories.files
                .map { normalizeFilePath(this, it) }
                .sorted()

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
