/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.net.URI
import java.nio.file.Files
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinWasmGradlePluginIT : AbstractKotlinWasmGradlePluginIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            wasmOptions = BuildOptions.WasmOptions(perModule = false)
        )

    @DisplayName("Check js target with browser")
    @GradleTest
    fun jsTargetWithBrowser(gradleVersion: GradleVersion) {
        jsTargetWithBrowser(gradleVersion, 1)
    }

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Check js target with binaryen per-module closed world")
    @GradleTest
    fun jsTargetWithBinaryenPerModuleClosedWorld(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify {
                it.replace("<JsEngine>", "d8")
            }

            buildScriptInjection {
                kotlinMultiplatform.wasmJs {
                    binaries.executable().forEach {
                        it.linkTask.configure {
                            compilerOptions.freeCompilerArgs.add("-Xwasm-generate-closed-world-multimodule")
                        }
                    }
                }
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")

                val original =
                    projectPath.resolve("build/compileSync/wasmJs/main/productionExecutable/kotlin")
                val optimized =
                    projectPath.resolve("build/compileSync/wasmJs/main/productionExecutable/optimized")

                original.listDirectoryEntries("*.wasm")
                    .also {
                        assertTrue {
                            it.size > 1
                        }
                    }
                    .forEach {
                        assertTrue {
                            Files.size(it) > Files.size(optimized.resolve(it.name))
                        }
                    }
            }

            build(":wasmJsD8ProductionRun") {
                assertTasksUpToDate(":compileProductionExecutableKotlinWasmJs")
                assertTasksUpToDate(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsD8ProductionRun")
            }

            projectPath.resolve("src/wasmJsMain/kotlin/foo.kt").modify {
                it.replace(
                    "println(foo())",
                    """println("Hello from Wasi")"""
                )
            }

            build(":wasmJsD8ProductionRun") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsD8ProductionRun")
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Check that wasm-js optimizer is executed if a dependent .wasm file is changed")
    @GradleTest
    fun dependencyChangeTriggersOptimizerWasmJsPerModuleClosedWorld(gradleVersion: GradleVersion) {
        project("wasm-js-per-module-closed-world", gradleVersion) {
            build(":app:wasmJsD8ProductionRun") {
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":app:wasmJsD8ProductionRun")
                assertOutputContains("Hello from Lib, App!")
            }

            subProject("mid").projectPath.resolve("src/wasmJsMain/kotlin/Mid.kt").modify {
                it.replace(
                    "fun greet(name: String): String = \"Hello from Lib, \$name!\"",
                    "fun greet(title: String): String = \"Hi from Lib, \$title!\""
                )
            }

            build(":app:wasmJsD8ProductionRun") {
                assertTasksExecuted(":mid:compileKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":app:wasmJsD8ProductionRun")
                assertOutputContains("Hi from Lib, App!")

                val kotlinDir = subProject("app").projectPath
                    .resolve("build/compileSync/wasmJs/main/productionExecutable/kotlin")
                assertTrue("mid.wasm should be present in multimodule output") {
                    kotlinDir.listDirectoryEntries("*mid*.wasm").isNotEmpty()
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @GradleTest
    @DisplayName("Changing lib triggers transitive recompilation through mid to app in per-module closed-world mode")
    fun libChangeTriggersTransitiveRecompileWasmJsPerModuleClosedWorld(gradleVersion: GradleVersion) {
        project("wasm-js-per-module-closed-world", gradleVersion) {
            subProject("mid").let {
                it.buildScriptInjection {
                    kotlinMultiplatform.sourceSets.getByName("wasmJsMain").dependencies {
                        api(project(":lib"))
                    }
                }
            }

            subProject("app").projectPath
                .resolve("src/wasmJsMain/kotlin/App.kt").modify {
                    """
                        fun main() {
                            val mid = Mid()
                            println(mid.greet("App"))
                            val lib = Lib()
                            val result = lib {
                                +"cube"
                                +"sphere"
                                +"light"
                            }
                            println(result)
                        }
                    """.trimIndent()
                }

            build(":app:wasmJsD8ProductionRun") {
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":app:wasmJsD8ProductionRun")
                assertOutputContains("Hello from Lib, App!")
                assertOutputContains("Scene: [cube, sphere, light]")

                val kotlinDir = subProject("app").projectPath
                    .resolve("build/compileSync/wasmJs/main/productionExecutable/kotlin")
                assertTrue("lib.wasm should be present in chain dependency output") {
                    kotlinDir.listDirectoryEntries("*lib*.wasm").isNotEmpty()
                }
                assertTrue("mid.wasm should be present in chain dependency output") {
                    kotlinDir.listDirectoryEntries("*mid*.wasm").isNotEmpty()
                }
            }

            subProject("lib").projectPath.resolve("src/wasmJsMain/kotlin/Lib.kt").modify {
                it.replace("\"Scene: [", "\"v2: [")
            }

            build(":app:wasmJsD8ProductionRun") {
                assertTasksExecuted(":lib:compileKotlinWasmJs")
                assertTasksExecuted(":mid:compileKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":app:wasmJsD8ProductionRun")
                assertOutputContains("Hello from Lib, App!")
                assertOutputContains("v2: [cube, sphere, light]")
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @GradleTest
    @DisplayName("Diamond dependency: lib.wasm appears exactly once despite two dependency paths")
    fun diamondDependencyWasmJsPerModuleClosedWorld(gradleVersion: GradleVersion) {
        project("wasm-js-per-module-closed-world", gradleVersion) {
            subProject("mid").buildScriptInjection {
                kotlinMultiplatform.sourceSets.getByName("wasmJsMain").dependencies {
                    api(project(":lib"))
                }
            }
            subProject("app").buildScriptInjection {
                kotlinMultiplatform.sourceSets.getByName("wasmJsMain").dependencies {
                    implementation(project(":lib"))
                }
            }
            subProject("app").projectPath
                .resolve("src/wasmJsMain/kotlin/App.kt").modify {
                    """
                    fun main() {
                        val lib = Lib()
                        val result = lib { +"cube"; +"sphere" }
                        println(result)
                    }
                    """.trimIndent()
                }

            build(":app:wasmJsD8ProductionRun") {
                assertTasksExecuted(":lib:compileKotlinWasmJs")
                assertTasksExecuted(":mid:compileKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":app:wasmJsD8ProductionRun")
                assertOutputContains("Scene: [cube, sphere]")

                val kotlinDir = subProject("app").projectPath
                    .resolve("build/compileSync/wasmJs/main/productionExecutable/kotlin")
                assertEquals(
                    1,
                    kotlinDir.listDirectoryEntries("*.wasm").filter {
                        it.name.contains("lib") && !it.name.contains("stdlib")
                    }.size,
                    "lib.wasm should appear exactly once in diamond dependency output"
                )
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @GradleTest
    @DisplayName("Third-party lib of a dependent module produces a separate .wasm in per-module closed world")
    fun thirdPartyDependencyWasmJsPerModuleClosedWorld(gradleVersion: GradleVersion) {
        project("wasm-js-per-module-closed-world", gradleVersion) {
            build(":app:wasmJsD8ProductionRun") {
                assertTasksExecuted(":app:wasmJsD8ProductionRun")
                assertOutputContains("Hello from Lib, App!")
            }

            subProject("mid").buildScriptInjection {
                kotlinMultiplatform.sourceSets.getByName("wasmJsMain").dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
                }
            }
            subProject("mid").projectPath
                .resolve("src/wasmJsMain/kotlin/Mid.kt").modify {
                    "import kotlinx.collections.immutable.persistentListOf\n\n" + it.replace(
                        "fun greet(name: String): String = \"Hello from Lib, \$name!\"",
                        "fun greet(name: String): String = persistentListOf(\"Hello from Lib\", name).toString()"
                    )
                }

            build(":app:wasmJsD8ProductionRun") {
                assertTasksExecuted(":mid:compileKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":app:wasmJsD8ProductionRun")
                assertOutputContains("[Hello from Lib, App]")

                val kotlinDir = subProject("app").projectPath
                    .resolve("build/compileSync/wasmJs/main/productionExecutable/kotlin")
                assertTrue("kotlinx-collections-immutable .wasm should be present in per-module output") {
                    kotlinDir.listDirectoryEntries("*.wasm").any { it.name.contains("collections-immutable") }
                }
            }
        }
    }
}

class KotlinWasmPerModuleGradlePluginIT : AbstractKotlinWasmGradlePluginIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            wasmOptions = BuildOptions.WasmOptions(perModule = true)
        )

    @DisplayName("Check js target with browser")
    @GradleTest
    fun jsTargetWithBrowser(gradleVersion: GradleVersion) {
        jsTargetWithBrowser(gradleVersion, 2)
    }

    @DisplayName("Check that js target all wasm files' size less in production")
    @GradleTest
    fun jsTargetWasmFilesSize(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify {
                it.replace("<JsEngine>", "browser")
            }

            val developmentSize: MutableMap<String, Long> = mutableMapOf()

            build("wasmJsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinWasmJs")

                projectPath.resolve("build/wasm/packages/redefined-wasm-module-name/kotlin").listDirectoryEntries()
                    .filter { it.extension == "wasm" }
                    .forEach { developmentSize[it.name] = it.fileSize() }
            }

            build("clean")

            build("wasmJsProductionExecutableCompileSync") {
                projectPath.resolve("build/wasm/packages/redefined-wasm-module-name/kotlin").listDirectoryEntries()
                    .filter { it.extension == "wasm" }
                    .forEach {
                        assertTrue("Production variant should be smaller than development variant for ${it.name}") {
                            it.fileSize() < developmentSize.getValue(it.name)
                        }
                    }
            }
        }
    }
}

@MppGradlePluginTests
abstract class AbstractKotlinWasmGradlePluginIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("Check wasi target")
    @GradleTest
    fun wasiTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            build(":wasmWasiTest") {
                assertTasksExecuted(":kotlinWasmNodeJsSetup")
                assertTasksExecuted(":compileKotlinWasmWasi")
                assertTasksExecuted(":wasmWasiNodeTest")
            }

            build(":wasmWasiTest") {
                assertTasksUpToDate(":kotlinWasmNodeJsSetup", ":compileKotlinWasmWasi", ":wasmWasiNodeTest")
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
                assertTasksFailed(":wasmWasiNodeTest")
            }

            build(":wasmWasiNodeProductionRun") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiOptimize")

                assertTasksAreNotInTaskGraph(":kotlinWasmToolingSetup")

                assertNoBuildWarnings()
            }
        }
    }

    @DisplayName("Check js target")
    @GradleTest
    fun jsTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-test", gradleVersion) {
            buildGradleKts.modify {
                it.replace("<JsEngine>", "nodejs")
            }

            kotlinSourcesDir("wasmJsTest").resolve("Test.kt").writeText(
                """
                    package my.pack.name

                    import kotlin.test.Test
                    import kotlin.test.assertEquals

                    class WasmTest {
                        @Test
                        fun test1() = assertEquals(foo(), 2)
                    }
                """.trimIndent()
            )

            build("build") {
                assertTasksExecuted(":kotlinWasmNodeJsSetup")
                assertTasksExecuted(":compileKotlinWasmJs")
                assertTasksExecuted(":wasmJsNodeTest")
            }

            build(":wasmJsTest") {
                assertTasksUpToDate(":kotlinWasmNodeJsSetup", ":compileKotlinWasmJs", ":wasmJsNodeTest")
            }
        }
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

    @DisplayName("Check wasi and js target")
    @GradleTest
    fun wasiAndJsTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-js-test", gradleVersion) {

            build("assemble") {
                assertTasksExecuted(":app:compileKotlinWasmWasi")
                assertTasksExecuted(":app:compileKotlinWasmJs")

                assertTasksExecuted(":lib:compileKotlinWasmWasi")
                assertTasksExecuted(":lib:compileKotlinWasmJs")
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

    @DisplayName("Check js target with binaryen")
    @GradleTest
    fun jsTargetWithBinaryen(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify {
                it.replace("<JsEngine>", "d8")
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")

                // we have such warning in Gradle 7, no warnings in later Gradle versions
                assertNoBuildWarnings(
                    setOf(
                        "This annotation should be used with the compiler argument '-opt-in=kotlin.RequiresOptIn'"
                    )
                )

                val original =
                    projectPath.resolve("build/compileSync/wasmJs/main/productionExecutable/kotlin/redefined-wasm-module-name.wasm")
                val optimized =
                    projectPath.resolve("build/compileSync/wasmJs/main/productionExecutable/optimized/redefined-wasm-module-name.wasm")
                assertTrue {
                    Files.size(original) > Files.size(optimized)
                }
            }

            build(":wasmJsD8ProductionRun") {
                assertTasksUpToDate(":compileProductionExecutableKotlinWasmJs")
                assertTasksUpToDate(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsD8ProductionRun")
            }

            projectPath.resolve("src/wasmJsMain/kotlin/foo.kt").modify {
                it.replace(
                    "println(foo())",
                    """println("Hello from Wasi")"""
                )
            }

            build(":wasmJsD8ProductionRun") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsD8ProductionRun")
            }
        }
    }

    @DisplayName("Check js target without sourcemap does not have warning")
    @GradleTest
    fun jsTargetWithoutSourceMap(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify {
                it.replace("<JsEngine>", "browser")
            }

            buildScriptInjection {
                @OptIn(ExperimentalWasmDsl::class)
                kotlinMultiplatform.wasmJs {
                    compilerOptions {
                        sourceMap.set(false)
                        sourceMapEmbedSources.convention(null as JsSourceMapEmbedMode?)
                    }
                }
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsBrowserDistribution")

                assertFileInProjectExists("build/compileSync/wasmJs/main/productionExecutable/kotlin/redefined-wasm-module-name.wasm")
                assertFileInProjectNotExists("build/compileSync/wasmJs/main/productionExecutable/kotlin/redefined-wasm-module-name.wasm.map")

                assertNoBuildWarnings(
                    setOf(
                        "This annotation should be used with the compiler argument '-opt-in=kotlin.RequiresOptIn'"
                    )
                )
            }
        }
    }

    protected fun jsTargetWithBrowser(gradleVersion: GradleVersion, filesCount: Int) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify {
                it.replace("<JsEngine>", "browser")
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsBrowserDistribution")

                assertFileInProjectExists("build/${Distribution.DIST}/wasmJs/productionExecutable/new-mpp-wasm-js.js")
                assertFileInProjectExists("build/${Distribution.DIST}/wasmJs/productionExecutable/new-mpp-wasm-js.js.map")

                assertFileContains(
                    projectPath.resolve(
                        "build/compileSync/wasmJs/main/productionExecutable/kotlin/redefined-wasm-module-name.wasm.map"
                    ),
                    "\"src/wasmJsMain/kotlin/foo.kt\"",
                    "\"NATIVE_IMPLEMENTATIONS.kt\"",
                )

                assertFileContains(
                    projectPath.resolve(
                        "build/wasm/packages/redefined-wasm-module-name/kotlin/redefined-wasm-module-name.wasm.map"
                    ),
                    "\"src/wasmJsMain/kotlin/foo.kt\"",
                    "\"NATIVE_IMPLEMENTATIONS.kt\"",
                )

                assertTrue("Expected ${filesCount} wasm file") {
                    projectPath.resolve("build/${Distribution.DIST}/wasmJs/productionExecutable").toFile().listFiles()!!
                        .filter { it.extension == "wasm" }
                        .size == filesCount
                }
            }

            projectPath.resolve("src/wasmJsMain/kotlin/foo.kt").replaceText(
                "actual fun foo(): Int = 2",
                "actual fun foo(): Int = 3",
            )

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsBrowserDistribution")

                assertFileInProjectExists("build/${Distribution.DIST}/wasmJs/productionExecutable/new-mpp-wasm-js.js")
                assertFileInProjectExists("build/${Distribution.DIST}/wasmJs/productionExecutable/new-mpp-wasm-js.js.map")

                assertTrue("Expected ${filesCount} wasm file") {
                    projectPath.resolve("build/${Distribution.DIST}/wasmJs/productionExecutable").toFile().listFiles()!!
                        .filter { it.extension == "wasm" }
                        .size == filesCount
                }
            }

            build("wasmJsBrowserDistribution") {
                assertTasksUpToDate(":kotlinWasmNpmInstall")
                assertTasksAreNotInTaskGraph(":kotlinNpmInstall")
            }

            build("jsBrowserDistribution") {
                assertTasksUpToDate(":kotlinNpmInstall")
                assertTasksAreNotInTaskGraph(":kotlinWasmNpmInstall")
            }
        }
    }

    @DisplayName("Check mix project with wasi only dependency works correctly")
    @GradleTest
    fun jsAndWasiTargetsWithDependencyOnWasiOnlyProject(gradleVersion: GradleVersion) {
        project("wasm-wasi-js-with-wasi-only-dependency", gradleVersion) {

            build("build") {
                assertTasksExecuted(":lib:compileKotlinWasmWasi")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinWasmWasi")
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


    @DisplayName("Browser print works with null type")
    @GradleTest
    @OsCondition(
        supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS],
        enabledOnCI = []
    ) // The CI can't run the test, so, ignore it until we add headless Chrome browser to our CI
    fun testBrowserNullPrint(gradleVersion: GradleVersion) {
        project("kt-63230", gradleVersion) {
            build("check", "-Pkotlin.tests.individualTaskReports=true") {
                assertTestResults(projectPath.resolve("TEST-wasm.xml"), "wasmJsBrowserTest")
            }
        }
    }

    @DisplayName("Wasm JS variant does not contain file:// in webpack and works in Node.JS")
    @GradleTest
    fun wasmJsImportMetaUrlLibrary(gradleVersion: GradleVersion) {
        project("mpp-wasm-js-browser-nodejs", gradleVersion) {

            build(":assemble", ":wasmJsNodeTest") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileKotlinWasmJs")
                assertTasksExecuted(":wasmJsNodeTest")
                assertTasksExecuted(":wasmJsBrowserDistribution")

                val dist = "build/dist/wasmJs/productionExecutable"
                val uninstantiatedFile = projectPath.resolve("$dist/mpp-wasm-js-browser-nodejs.js")
                assertFileExists(uninstantiatedFile)
                assertFileDoesNotContain(uninstantiatedFile, "file://")
            }
        }
    }

    @DisplayName("Wasm sync task copies main compilation resources to test NPM package")
    @GradleTest
    fun wasmSyncTaskCopiesMainResourcesToTest(gradleVersion: GradleVersion) {
        project("mpp-wasm-js-browser-nodejs", gradleVersion) {
            build(":wasmJsNodeTest") {
                assertTasksExecuted(":wasmJsNodeTest")
                assertTasksExecuted(":wasmJsTestTestDevelopmentExecutableCompileSync")

                val packageDir = "build/wasm/packages/mpp-wasm-js-browser-nodejs-test/kotlin"
                assertFileExists(projectPath.resolve("$packageDir/data.json"))
                assertFileExists(projectPath.resolve("$packageDir/data-test.json"))
            }
        }
    }

    @DisplayName("Test compilation package.json generation depends on main package.json generation")
    @GradleTest
    fun testPackageJsonDependsOnMainPackageJson(gradleVersion: GradleVersion) {
        project("mpp-wasm-js-browser-nodejs", gradleVersion) {
            build(":wasmRootPackageJson") {
                assertTasksExecuted(":wasmJsPackageJson")
                assertTasksExecuted(":wasmJsTestPackageJson")
                assertTasksAreNotInTaskGraph(":compileKotlinWasmJs")
                assertTasksAreNotInTaskGraph(":compileTestKotlinWasmJs")
            }
        }
    }

    @DisplayName("Extracting NPM dependencies from transitive dependency")
    @GradleTest
    fun testExtractingNpmDependenciesFromTransitive(gradleVersion: GradleVersion) {
        project("mpp-wasm-published-library", gradleVersion) {
            val settingsGradleText = settingsGradleKts.readText()
            settingsGradleKts.append("include(\":library\")")
            build(":library:publish") {
                assertTasksExecuted(
                    ":library:compileKotlinWasmJs"
                )

                val moduleDir = subProject("library")
                    .projectPath
                    .resolve("repo/com/example/mpp-wasm-published-library/library-wasm-js/0.0.1/")

                val publishedKlib = moduleDir.resolve("library-wasm-js-0.0.1.klib")

                assertFileExists(publishedKlib)
            }

            build("clean")

            settingsGradleKts.writeText(settingsGradleText)
            settingsGradleKts.append("include(\":app\")")
            settingsGradleKts.append(
                """
                dependencyResolutionManagement {
                    repositories {
                        maven(rootDir.resolve("library/repo").toURI())
                    }
                }
                """.trimIndent()
            )

            build(":wasmRootPackageJson") {
                assertTasksExecuted(
                    ":wasmRootPackageJson"
                )

                val moduleDir = projectPath
                    .resolve("build/wasm/packages_imported/Kotlin-DateTime-library-kotlinx-datetime-wasm-js/0.6.0")

                val kotlinxDatetimePackageJson = moduleDir.resolve("package.json")

                assertFileExists(kotlinxDatetimePackageJson)
            }
        }
    }

    @DisplayName("Browser case works correctly with custom formatters")
    @GradleTest
    fun testWasmCustomFormattersUsage(gradleVersion: GradleVersion) {
        project("wasm-browser-simple-project", gradleVersion) {
            buildGradleKts.append(
                //language=Kotlin
                """
                |
                | tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
                |    compilerOptions.freeCompilerArgs.add("-Xwasm-debugger-custom-formatters")
                | }
                """.trimMargin()
            )

            build("wasmJsBrowserDistribution") {
                assertTasksExecuted(":compileKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
            }
        }
    }

    @DisplayName("Changed output module name")
    @GradleTest
    fun testChangedOutputModuleName(gradleVersion: GradleVersion) {
        project("wasm-browser-simple-project", gradleVersion) {
            val moduleName = "hello"
            buildGradleKts.modify {
                it.replace(
                    "wasmJs {",
                    """
                        wasmJs {
                            outputModuleName.set("$moduleName")
                    """.trimIndent()
                )
            }

            build("assemble") {
                assertFileExists(
                    projectPath.resolve("build/compileSync/wasmJs/main/productionExecutable/kotlin/$moduleName.mjs")
                )
            }
        }
    }

    @DisplayName("webpack configuration is valid")
    @GradleTest
    fun testWebpackConfig(gradleVersion: GradleVersion) {
        project("kotlin-js-test-webpack-config", gradleVersion) {
            build("wasmJsBrowserDevelopmentWebpack")

            build("wasmJsCheckConfigDevelopmentWebpack")

            build("wasmJsCheckConfigProductionWebpack")

            build("wasmJsCheckConfigDevelopmentRun")

            build("wasmJsCheckConfigProductionRun")
        }
    }

    // Android Studio touches all properties to analyze
    // It may break some properties with Task source
    // but we need to work in Android Studio,
    // so it is better to be sure that we work in this strange situation
    @DisplayName("Touch properties to not break Android studio")
    @GradleTest
    fun testTouchingWebpackPropertyToNotBreakAndroidStudio(gradleVersion: GradleVersion) {
        project("wasm-browser-simple-project", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("wasmJsBrowserDevelopmentRun")                    
                |    .get()
                |    .inputFilesDirectory
                |    .get()
                |""".trimMargin()
            )

            build("help")
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Different binaryen versions per project")
    @GradleTest
    fun testDifferentBinaryenVersions(gradleVersion: GradleVersion) {
        project("wasm-browser-several-modules", gradleVersion) {
            val binaryenVersionForFoo = "123"
            val binaryenVersionForBar = "119"
            subProject("foo").let {
                it.buildScriptInjection {
                    project.extensions.getByType(BinaryenEnvSpec::class.java).version.set(binaryenVersionForFoo)
                }
            }

            subProject("bar").let {
                it.buildScriptInjection {
                    project.extensions.getByType(BinaryenEnvSpec::class.java).version.set(binaryenVersionForBar)
                }
            }

            build(":foo:compileProductionExecutableKotlinWasmJsOptimize") {
                assertOutputContains(
                    Path("binaryen-version_$binaryenVersionForFoo").resolve("bin").resolve("wasm-opt").pathString
                )
            }

            build(":bar:compileProductionExecutableKotlinWasmJsOptimize") {
                assertOutputContains(
                    Path("binaryen-version_$binaryenVersionForBar").resolve("bin").resolve("wasm-opt").pathString
                )
            }
        }
    }

    @DisplayName("Check js target test without kotlin-test")
    @GradleTest
    fun nodejsTestWithoutKotlinTest(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify {
                it.replace(
                    "<JsEngine> {",
                    // filter is necessary to mute Gradle error
                    // https://docs.gradle.org/8.12.1/userguide/upgrading_version_8.html#test_task_fail_on_no_test_executed
                    """
                        |nodejs {
                        |    testTask {
                        |        filter.isFailOnNoMatchingTests = false
                        |        filter.includeTest("Foo", "bar")
                        |    }
                    """.trimMargin()
                )
            }

            projectPath.resolve("src/wasmJsTest/kotlin/foo.kt").apply {
                toFile().parentFile.mkdirs()
                writeText(
                    """
                    fun foo() = 73
                """.trimIndent()
                )
            }

            build("wasmJsTest") {
                assertTasksExecuted(":wasmJsTest")
            }
        }
    }

    @DisplayName("Check js target webpack config changes reflected in webpack task properties")
    @GradleTest
    fun webpackConfigChangesReflectedInWebpackTask(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify {
                it.replace(
                    "<JsEngine>",
                    "browser"
                )
            }

            @OptIn(ExperimentalWasmDsl::class)
            buildScriptInjection {
                kotlinMultiplatform.wasmJs {
                    browser {
                        webpackTask {
                            it.generateConfigOnly = true
                            it.devServerProperty.set(
                                KotlinWebpackConfig.DevServer(
                                    static = mutableListOf("foo")
                                )
                            )
                        }

                        commonWebpackConfig {
                            it.outputFileName = "check.js"
                            it.devServer = (it.devServer ?: KotlinWebpackConfig.DevServer()).apply {
                                @Suppress("DEPRECATION")
                                static = (static ?: mutableListOf()).apply {
                                    add("bar")
                                }
                            }
                        }

                        runTask {
                            it.devServerProperty.set(it.devServerProperty.get().copy())

                            if (it.devServerProperty.get().statics.isEmpty()) {
                                error("No dev server statics after copying of data class")
                            }
                        }
                    }
                }

                project.tasks.withType(KotlinWebpack::class.java).named("wasmJsBrowserProductionWebpack") {
                    it.doLast { task ->
                        task as KotlinWebpack
                        println("File output name: " + task.mainOutputFileName.get())
                        val pathConfig = task.configFile.get().toPath()
                        println("Path config: " + pathConfig)
                    }
                }
            }

            build("wasmJsBrowserProductionWebpack") {
                assertTasksExecuted(":wasmJsBrowserProductionWebpack")

                assertOutputContains("File output name: check.js")
                val pathConfig = output.substringAfter("Path config: ").substringBefore("webpack.config.js")
                assertFileContains(
                    Path(pathConfig).resolve("webpack.config.js"),
                    "\"static\": [\n" +
                            "    \"foo\",\n" +
                            "    \"bar\"\n" +
                            "  ]"
                )
            }
        }
    }

    @DisplayName("wasm js composite build works")
    @GradleTest
    fun testWasmJsCompositeBuild(gradleVersion: GradleVersion) {
        project(
            "wasm-composite-build",
            gradleVersion,
            // `:compileKotlinWasmJs` task is not compatible with CC on Gradle 7
            buildOptions = defaultBuildOptions.disableConfigurationCacheForGradle7(gradleVersion),
        ) {
            fun BuildResult.moduleVersion(rootModulePath: String, moduleName: String): String =
                projectPath.resolve(rootModulePath).toFile()
                    .resolve(NpmProject.PACKAGE_JSON)
                    .also {
                        if (!it.exists()) {
                            it
                                .parentFile // lib2
                                .parentFile // node_modules
                                .parentFile // js
                                .resolve(NpmProject.PACKAGE_JSON)
                                .let {
                                    println("root package.json:")
                                    println(it.readText())
                                }

                            it
                                .parentFile // lib2
                                .parentFile // node_modules
                                .parentFile // js
                                .resolve("packages_imported")
                                .also {
                                    println("ALL IMPORTED: ")
                                    it.listFiles()
                                        ?.forEach {
                                            println(it.absolutePath)
                                        }
                                    it.resolve(".visited-gradle").let {
                                        println("visited-gradle content")
                                        println(it.readText())
                                    }
                                }
                                .resolve("lib2")
                                .resolve("0.0.0-unspecified")
                                .resolve(NpmProject.PACKAGE_JSON)
                                .let {
                                    println("lib2 package.json state:")
                                    if (it.exists()) {
                                        println(it.readText())
                                    } else {
                                        println("lib2 package.json does not exists")
                                    }
                                }

                            printBuildOutput()
                        }
                    }
                    .let { fromSrcPackageJson(it) }
                    .let { it?.dependencies }
                    ?.getValue(moduleName)
                    ?: error("Not found package $moduleName in $rootModulePath")

            build("build") {
                val libDecamelizeVersion = moduleVersion("build/wasm/node_modules/lib-lib-2", "decamelize")
                assertEquals("1.1.1", libDecamelizeVersion)

                val libAsyncVersion = moduleVersion("build/wasm/node_modules/lib-lib-2", "async")
                assertEquals("2.6.2", libAsyncVersion)

                val appNodeFetchVersion = moduleVersion("build/wasm/node_modules/wasm-composite-build", "node-fetch")
                assertEquals("3.2.8", appNodeFetchVersion)
            }
        }
    }

    @DisplayName("when project has FAIL_ON_PROJECT_REPOS, expect Kotlin/Wasm tools are downloaded correctly")
    @GradleTest
    fun testFailOnProjectReposUsingCustomRepo(gradleVersion: GradleVersion) {
        // Gradle versions below 8.1 do not correctly support repository mode
        val dependencyManagement =
            if (gradleVersion <= GradleVersion.version("8.1")) {
                DependencyManagement.DisabledDependencyManagement
            } else {
                DependencyManagement.DefaultDependencyManagement()
            }

        project(
            "wasm-project-repos",
            gradleVersion,
            dependencyManagement = dependencyManagement
        ) {

            settingsBuildScriptInjection {
                settings.dependencyResolutionManagement.repositories.apply {
                    ivy { repo ->
                        repo.name = "Node.JS dist"
                        repo.url = URI("https://nodejs.org/dist")
                        repo.patternLayout {
                            it.artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                        }
                        repo.metadataSources {
                            it.artifact()
                        }
                        repo.content {
                            it.includeModule("org.nodejs", "node")
                        }
                    }
                    ivy { repo ->
                        repo.name = "Yarn dist"
                        repo.url = URI("https://github.com/yarnpkg/yarn/releases/download")
                        repo.patternLayout {
                            it.artifact("v[revision]/[artifact](-v[revision]).[ext]")
                        }
                        repo.metadataSources {
                            it.artifact()
                        }
                        repo.content {
                            it.includeModule("com.yarnpkg", "yarn")
                        }
                    }
                    ivy { repo ->
                        repo.name = "Binaryen dist"
                        repo.url = URI("https://github.com/WebAssembly/binaryen/releases/download")
                        repo.patternLayout {
                            it.artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
                        }
                        repo.metadataSources {
                            it.artifact()
                        }
                        repo.content {
                            it.includeModule("com.github.webassembly", "binaryen")
                        }
                    }
                    ivy { repo ->
                        repo.name = "D8 dist"
                        repo.url = URI("https://storage.googleapis.com/chromium-v8/official/canary")
                        repo.patternLayout {
                            it.artifact("[artifact]-[revision].[ext]")
                        }
                        repo.metadataSources {
                            it.artifact()
                        }
                        repo.content {
                            it.includeModule("google.d8", "v8")
                        }
                    }
                }
            }

            build("kotlinWasmNodeJsSetup", "kotlinWasmYarnSetup", "kotlinWasmBinaryenSetup", "kotlinWasmD8Setup") {
                assertTasksExecuted(":kotlinWasmNodeJsSetup")
                assertTasksExecuted(":kotlinWasmYarnSetup")
                assertTasksExecuted(":kotlinWasmBinaryenSetup")
                assertTasksExecuted(":kotlinWasmD8Setup")
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("when project has FAIL_ON_PROJECT_REPOS without downloading tools, expect KGP does not download tools")
    @GradleTest
    fun testFailOnProjectReposNoDownload(gradleVersion: GradleVersion) {
        // Gradle versions below 8.1 do not correctly support repository mode
        val dependencyManagement =
            if (gradleVersion <= GradleVersion.version("8.1")) {
                DependencyManagement.DisabledDependencyManagement
            } else {
                DependencyManagement.DefaultDependencyManagement()
            }

        project(
            "wasm-project-repos",
            gradleVersion,
            dependencyManagement = dependencyManagement
        ) {
            @Suppress("UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS")
            buildScriptInjection {
                project.plugins.withType(WasmNodeJsPlugin::class.java) {
                    project.extensions.getByType(WasmNodeJsEnvSpec::class.java).download.set(false)
                }

                project.plugins.withType(WasmYarnPlugin::class.java) {
                    project.extensions.getByType(WasmYarnRootEnvSpec::class.java).download.set(false)
                }

                project.plugins.withType(BinaryenPlugin::class.java) {
                    project.extensions.getByType(BinaryenEnvSpec::class.java).download.set(false)
                }

                project.plugins.withType(D8Plugin::class.java) {
                    project.extensions.getByType(D8EnvSpec::class.java).download.set(false)
                }
            }

            build("kotlinWasmNodeJsSetup", "kotlinWasmYarnSetup", "kotlinWasmBinaryenSetup", "kotlinWasmD8Setup") {
                assertTasksSkipped(":kotlinWasmNodeJsSetup")
                assertTasksSkipped(":kotlinWasmYarnSetup")
                assertTasksSkipped(":kotlinWasmBinaryenSetup")
                assertTasksSkipped(":kotlinWasmD8Setup")
            }
        }
    }
}
