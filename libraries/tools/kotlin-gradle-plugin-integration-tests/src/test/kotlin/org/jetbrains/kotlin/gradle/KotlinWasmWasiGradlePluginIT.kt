/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.append
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.assertFileInProjectExists
import org.jetbrains.kotlin.gradle.testbase.assertNoBuildWarnings
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertTasksAreNotInTaskGraph
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.assertTasksFailed
import org.jetbrains.kotlin.gradle.testbase.assertTasksUpToDate
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.disableIsolatedProjectsBecauseOfJsAndWasmKT75899
import org.jetbrains.kotlin.gradle.testbase.modify
import org.jetbrains.kotlin.gradle.testbase.printBuildOutput
import org.jetbrains.kotlin.gradle.testbase.project
import org.junit.jupiter.api.DisplayName
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.text.replace

@MppGradlePluginTests
class KotlinWasmWasiGradlePluginIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("Check wasi target")
    @GradleTest
    fun wasiTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            wasmWasiTest("Node", "NodeJs")
        }
    }

    @DisplayName("Check wasi target with wasmtime")
    @GradleTest
    fun wasiWasmtimeTarget(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            wasmWasiTest("Wasmtime")
        }
    }

    private fun TestProject.wasmWasiTest(engine: String, engineSetup: String = engine) {
        build(":wasmWasiTest") {
            assertTasksExecuted(":kotlinWasm${engineSetup}Setup")
            assertTasksExecuted(":compileKotlinWasmWasi")
            assertTasksExecuted(":wasmWasi${engine}Test")
        }

        build(":wasmWasiTest") {
            assertTasksUpToDate(":kotlinWasm${engineSetup}Setup", ":compileKotlinWasmWasi", ":wasmWasi${engine}Test")
        }

        projectPath.resolve("src/wasmWasiTest/kotlin/Test.kt").modify {
            it.replace(
                "fun test2() = assertEquals(foo(), 2)",
                """
                    |fun test2() = assertEquals(foo(), 2)
                    |
                    |@Test
                    |fun test3() = assertEquals(foo(), 3)
                    |""".trimMargin()
            )
        }

        buildAndFail(":wasmWasiTest") {
            assertTasksUpToDate(":compileKotlinWasmWasi")
            assertTasksFailed(":wasmWasi${engine}Test")
        }

        build(":wasmWasi${engine}ProductionRun") {
            assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
            assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiOptimize")
            if (engine == "Wasmtime") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiComponentize")
            }

            assertTasksAreNotInTaskGraph(":kotlinWasmToolingSetup")

            assertNoBuildWarnings()
        }
    }

    @DisplayName("Check wasi target with wasmtime and component model basic hello world")
    @GradleTest
    fun wasiWasmtimeBasicComponentModel(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiOptimize",
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)
                assertComponentNew(
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString(),
                    component = projectPath.resolve(componentFile)
                )
            }

            build(":wasmWasiWasmtimeDevelopmentRun") {
                assertTasksExecuted(
                    ":compileDevelopmentExecutableKotlinWasmWasi",
                    ":compileDevelopmentExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeDevelopmentRun"
                )
                assertOutputContains("Hello from Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/developmentExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)
                assertComponentNew(
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/developmentExecutable/kotlin/wasm-wasmtime-test.wasm")
                        .loggedPathString(),
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Check wasi target with wasmtime and wit declarations in the project")
    @GradleTest
    fun wasiWasmtimeWithProjectWitDeclarations(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            val witDir = projectPath.resolve("wit")
            witDir.createDirectories()
            witDir.resolve("world.wit").writeText(
                """
                package my:project;

                world my-world {
                    import print: func(msg: string);
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = witDir,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksUpToDate(
                    ":compileProductionExecutableKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiOptimize",
                    ":compileProductionExecutableKotlinWasmWasiComponentize"
                )
                assertTasksExecuted(":wasmWasiWasmtimeProductionRun")
            }

            witDir.resolve("world.wit").writeText(
                """
                package my:project;

                world my-world {
                    import print: func(msg: string);
                    import log: func(msg: string);
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksUpToDate(
                    ":compileProductionExecutableKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiOptimize"
                )
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )

                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = witDir,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Check wasi target with wasmtime and wit declarations extracted from klib")
    @GradleTest
    fun wasiWasmtimeWithKlibWitDeclarations(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            val libDir = projectPath.resolve("lib").createDirectories()
            libDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    kotlin("multiplatform")
                    `maven-publish`
                }

                group = "com.example.wasmlib"
                version = "0.0.1"

                kotlin {
                    wasmWasi()
                }

                publishing {
                    repositories {
                        maven(rootDir.resolve("repo"))
                    }
                }
                """.trimIndent()
            )

            libDir.resolve("src/wasmWasiMain/kotlin/Lib.kt").apply {
                parent.createDirectories()
                writeText(
                    """
                    package com.example.wasmlib

                    fun getGreeting(): String = "Hello from Lib"
                    """.trimIndent()
                )
            }

            val settingsGradleText = settingsGradleKts.readText()
            settingsGradleKts.append("include(\":lib\")")

            build(":lib:publish") {
                assertTasksExecuted(":lib:compileKotlinWasmWasi")
            }

            val publishedKlib = projectPath
                .resolve("repo/com/example/wasmlib/lib-wasm-wasi/0.0.1/lib-wasm-wasi-0.0.1.klib")
            assertFileExists(publishedKlib)

            FileSystems.newFileSystem(publishedKlib, null as ClassLoader?).use { fs ->
                val witFolder = fs.getPath("wit")
                Files.createDirectories(witFolder)
                Files.write(
                    witFolder.resolve("dep.wit"),
                    """
                    package com:example;

                    world dep {
                        import log: func(param: string);
                    }
                    """.trimIndent().toByteArray()
                )
            }

            settingsGradleKts.writeText(settingsGradleText)
            settingsGradleKts.append(
                """
                dependencyResolutionManagement {
                    repositories {
                        maven(rootDir.resolve("repo").toURI())
                    }
                }
                """.trimIndent()
            )

            buildGradleKts.modify {
                it.replace(
                    "sourceSets {",
                    """
                    sourceSets {
                        getByName("wasmWasiMain") {
                            dependencies {
                                implementation("com.example.wasmlib:lib-wasm-wasi:0.0.1")
                            }
                        }
                    """.trimIndent()
                )
            }

            projectPath.resolve("src/wasmWasiMain/kotlin/foo.kt").writeText(
                """
                package my.pack.name

                import com.example.wasmlib.getGreeting

                fun main() {
                    println(getGreeting() + ", Wasi")
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Lib, Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProjectPattern = "[^\\r\\n]+lib-wasm-wasi[^\\r\\n]*-wit",
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Check wasi target with wasmtime and wit declarations from project dependency")
    @GradleTest
    fun wasiWasmtimeWithProjectDependencyWitDeclarations(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            val libDir = projectPath.resolve("lib").createDirectories()
            libDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    kotlin("multiplatform")
                }

                kotlin {
                    wasmWasi()
                }
                """.trimIndent()
            )

            libDir.resolve("src/wasmWasiMain/kotlin/Lib.kt").apply {
                parent.createDirectories()
                writeText(
                    """
                    package com.example.wasmlib

                    fun getLibGreeting(): String = "Hello from Project Lib"
                    """.trimIndent()
                )
            }

            val libWit = libDir.resolve("wit")
            libWit.createDirectories()
            libWit.resolve("lib.wit").writeText(
                """
                package my:lib;

                world lib-world {
                    import lib-log: func(msg: string);
                }
                """.trimIndent()
            )

            settingsGradleKts.append("include(\":lib\")")

            val appWit = projectPath.resolve("wit")
            appWit.createDirectories()
            appWit.resolve("app.wit").writeText(
                """
                package my:app;

                world app-world {
                    import app-log: func(msg: string);
                }
                """.trimIndent()
            )

            buildGradleKts.modify {
                it.replace(
                    "sourceSets {",
                    """
                    sourceSets {
                        getByName("wasmWasiMain") {
                            dependencies {
                                implementation(project(":lib"))
                            }
                        }
                    """.trimIndent()
                )
            }

            projectPath.resolve("src/wasmWasiMain/kotlin/foo.kt").writeText(
                """
                package my.pack.name

                import com.example.wasmlib.getLibGreeting

                fun main() {
                    println(getLibGreeting() + ", Wasi")
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":lib:compileKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Project Lib, Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val moduleWithLibWit = assertComponentEmbed(
                    witProject = libWit,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                val moduleWithAllWits = assertComponentEmbed(
                    witProject = appWit,
                    inputModule = moduleWithLibWit
                )

                assertComponentNew(
                    inputModule = moduleWithAllWits,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Explicitly imported WASI component model function")
    @GradleTest
    fun wasiWasmtimeWithExplicitlyImportedWasiFunction(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            val appWit = projectPath.resolve("wit")
            appWit.createDirectories()
            appWit.resolve("app.wit").writeText(
                """
                package my:app;

                world app-world {
                    import wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION;
                }
                """.trimIndent()
            )
            appWit.addMonotonicClockWitDependency()

            projectPath.resolve("src/wasmWasiMain/kotlin/foo.kt").writeText(
                """
                package my.pack.name

                import kotlin.wasm.ExperimentalWasmInterop
                import kotlin.wasm.WasmImport

                @OptIn(ExperimentalWasmInterop::class)
                @WasmImport("wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION", "now")
                private external fun monotonicNow(): Long

                fun main() {
                    println("Monotonic clock is " + if (monotonicNow() <= monotonicNow()) "monotonic" else "broken")
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Monotonic clock is monotonic")

                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = appWit,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("WASI component model function explicitly imported by a dependency")
    @GradleTest
    fun wasiWasmtimeWithWasiFunctionImportedByDependency(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            val libDir = projectPath.resolve("lib").createDirectories()
            libDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    kotlin("multiplatform")
                }

                kotlin {
                    wasmWasi()
                }
                """.trimIndent()
            )

            libDir.resolve("src/wasmWasiMain/kotlin/Lib.kt").apply {
                parent.createDirectories()
                writeText(
                    """
                    package com.example.wasmlib

                    import kotlin.wasm.ExperimentalWasmInterop
                    import kotlin.wasm.WasmImport

                    @OptIn(ExperimentalWasmInterop::class)
                    @WasmImport("wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION", "now")
                    private external fun monotonicNow(): Long

                    fun isClockMonotonic(): Boolean = monotonicNow() <= monotonicNow()
                    """.trimIndent()
                )
            }

            val libWit = libDir.resolve("wit")
            libWit.createDirectories()
            libWit.resolve("lib.wit").writeText(
                """
                package my:lib;

                world lib-world {
                    import wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION;
                }
                """.trimIndent()
            )
            libWit.addMonotonicClockWitDependency()

            settingsGradleKts.append("include(\":lib\")")

            buildGradleKts.modify {
                it.replace(
                    "sourceSets {",
                    """
                    sourceSets {
                        getByName("wasmWasiMain") {
                            dependencies {
                                implementation(project(":lib"))
                            }
                        }
                    """.trimIndent()
                )
            }

            projectPath.resolve("src/wasmWasiMain/kotlin/foo.kt").writeText(
                """
                package my.pack.name

                import com.example.wasmlib.isClockMonotonic

                fun main() {
                    println("Monotonic clock is " + if (isClockMonotonic()) "monotonic" else "broken")
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":lib:compileKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Monotonic clock is monotonic")

                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = libWit,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    /**
     * Declares the `wasi:clocks` package as a dependency of this WIT project,
     * because every WIT project has to be self-contained.
     *
     * Only the `now` function of the `monotonic-clock` interface is declared,
     * because a component may import a subset of the functions of an interface provided by the host.
     */
    private fun Path.addMonotonicClockWitDependency() {
        val clocksPackage = resolve("deps/clocks")
        clocksPackage.createDirectories()
        clocksPackage.resolve("monotonic-clock.wit").writeText(
            """
            package wasi:clocks@$WASI_PACKAGES_VERSION;

            interface monotonic-clock {
                type instant = u64;

                now: func() -> instant;
            }
            """.trimIndent()
        )
    }

    private fun Path.loggedPathString(): String =
        toRealPath().absolutePathString()

    /**
     * Asserts that `wasm-tools component embed` embedded [witProject] into [inputModule]
     * and returns the path of the module with the embedded WIT declarations.
     */
    private fun BuildResult.assertComponentEmbed(witProject: Path, inputModule: String): String =
        assertComponentEmbed(Regex.escape(witProject.loggedPathString()), inputModule)

    /**
     * Asserts that `wasm-tools component embed` embedded a WIT project
     * whose path matches the [witProjectPattern] regular expression into [inputModule]
     * and returns the path of the module with the embedded WIT declarations.
     *
     * WIT projects extracted from klibs are located in the Gradle artifact transforms cache,
     * so their paths can be matched only by a pattern.
     */
    private fun BuildResult.assertComponentEmbed(witProjectPattern: String, inputModule: String): String {
        val embedCommand = Regex(
            "component embed $witProjectPattern " + Regex.escape(inputModule) + Regex.escape(" -o ") + "([^\\r\\n]+)"
        )
        val embed = embedCommand.find(output)
        if (embed == null) printBuildOutput()
        assertNotNull(embed, "Build output does not contain any line matching '$embedCommand' regex.")
        return embed.groupValues[1]
    }

    /**
     * Asserts that `wasm-tools component new` turned [inputModule] into [component].
     */
    private fun BuildResult.assertComponentNew(inputModule: String, component: Path) {
        assertOutputContains(
            Regex(
                Regex.escape("component new $inputModule") +
                        " --adapt wasi_snapshot_preview1=[^\\r\\n]+ -o " +
                        Regex.escape(component.loggedPathString())
            )
        )
    }

    @DisplayName("Check wasi target run")
    @GradleTest
    fun wasiRun(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            build(":wasmWasiNodeDevelopmentRun") {
                assertOutputContains("Hello from Wasi")
            }
        }
    }

    @DisplayName("Check wasi target with binaryen")
    @GradleTest
    fun wasiTargetWithBinaryen(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            buildGradleKts.modify {
                it.replace("wasmWasi {", "wasmWasi {\nbinaries.executable()")
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiOptimize")

                val original =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/kotlin/new-mpp-wasm-wasi-test.wasm")
                val optimized =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/new-mpp-wasm-wasi-test.wasm")
                assertTrue {
                    Files.size(original) > Files.size(optimized)
                }
            }
        }
    }

    @DisplayName("Wasi library")
    @GradleTest
    fun wasiLibrary(gradleVersion: GradleVersion) {
        project("wasm-wasi-library", gradleVersion) {

            build(":build") {
                assertTasksExecuted(":compileProductionLibraryKotlinWasmWasi")
                assertTasksExecuted(":compileKotlinWasmWasi")
                assertTasksExecuted(":wasmWasiNodeTest")
                assertTasksExecuted(":wasmWasiNodeProductionLibraryDistribution")

                val dist = "build/dist/wasmWasi/productionLibrary"
                assertFileExists(projectPath.resolve("$dist/foo.txt"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library.wasm"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library.wasm.map"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library.mjs"))
            }
        }
    }
}

private const val WASI_PACKAGES_VERSION = "0.2.12"
