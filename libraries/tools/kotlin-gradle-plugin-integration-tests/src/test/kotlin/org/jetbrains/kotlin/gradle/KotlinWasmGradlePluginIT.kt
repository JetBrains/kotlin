/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertTrue

@MppGradlePluginTests
class KotlinWasmGradlePluginIT : KGPBaseTest() {

    @DisplayName("Check wasi target")
    @GradleTest
    fun wasiTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build(":wasmWasiTest") {
                assertTasksExecuted(":kotlinNodeJsSetup")
                assertTasksExecuted(":compileKotlinWasmWasi")
                assertTasksExecuted(":wasmWasiNodeTest")
            }

            build(":wasmWasiTest") {
                assertTasksUpToDate(":kotlinNodeJsSetup", ":compileKotlinWasmWasi", ":wasmWasiNodeTest")
            }

            projectPath.resolve("src/wasmWasiTest/kotlin/Test.kt").modify {
                it.replace(
                    "fun test2() = assertEquals(foo(), 2)",
                    "fun test2() = assertEquals(foo(), 2)" + "\n" +
                            """
                            @Test
                            fun test3() = assertEquals(foo(), 3)
                            """
                )
            }

            buildAndFail(":wasmWasiTest") {
                assertTasksUpToDate(":compileKotlinWasmWasi")
                assertTasksFailed(":wasmWasiNodeTest")
            }

            build(":wasmWasiNodeProductionRun") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiOptimize")
            }
        }
    }

    @DisplayName("Check js target")
    @GradleTest
    fun jsTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-test", gradleVersion) {
            buildGradleKts.modify {
                transformBuildScriptWithPluginsDsl(it)
                    .replace("<JsEngine>", "nodejs")
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
                assertTasksExecuted(":kotlinNodeJsSetup")
                assertTasksExecuted(":compileKotlinWasmJs")
                assertTasksExecuted(":wasmJsNodeTest")
            }

            build(":wasmJsTest") {
                assertTasksUpToDate(":kotlinNodeJsSetup", ":compileKotlinWasmJs", ":wasmJsNodeTest")
            }
        }
    }

    @DisplayName("Check wasi target run")
    @GradleTest
    fun wasiRun(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build(":wasmWasiNodeDevelopmentRun") {
                assertOutputContains("Hello from Wasi")
            }
        }
    }

    @DisplayName("Check wasi and js target")
    @GradleTest
    fun wasiAndJsTarget(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-js-test", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

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
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            buildGradleKts.modify {
                it.replace("wasmWasi {", "wasmWasi {\napplyBinaryen()\nbinaries.executable()")
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiOptimize")

                val original =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/kotlin/new-mpp-wasm-wasi-test-wasm-wasi.wasm")
                val optimized =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/new-mpp-wasm-wasi-test-wasm-wasi.wasm")
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
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            buildGradleKts.modify {
                it.replace("<JsEngine>", "d8")
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")

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
                    "fun main() {}",
                    """
                            fun main() {
                                println("Hello from Wasi")
                            }
                            """
                )
            }

            build(":wasmJsD8ProductionRun") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsD8ProductionRun")
            }
        }
    }

    @DisplayName("Check js target with browser")
    @GradleTest
    fun jsTargetWithBrowser(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-js", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            buildGradleKts.modify {
                it.replace("<JsEngine>", "browser")
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJs")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmJsOptimize")
                assertTasksExecuted(":wasmJsBrowserDistribution")

                assertFileInProjectExists("build/${Distribution.DIST}/wasmJs/productionExecutable/redefined-wasm-module-name.wasm")
                assertFileInProjectExists("build/${Distribution.DIST}/wasmJs/productionExecutable/new-mpp-wasm-js.js")
                assertFileInProjectExists("build/${Distribution.DIST}/wasmJs/productionExecutable/new-mpp-wasm-js.js.map")
            }
        }
    }

    @DisplayName("Check mix project with wasi only dependency works correctly")
    @GradleTest
    fun jsAndWasiTargetsWithDependencyOnWasiOnlyProject(gradleVersion: GradleVersion) {
        project("wasm-wasi-js-with-wasi-only-dependency", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

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
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build(":build") {
                assertTasksExecuted(":compileProductionLibraryKotlinWasmWasi")
                assertTasksExecuted(":compileKotlinWasmWasi")
                assertTasksExecuted(":wasmWasiNodeTest")
                assertTasksExecuted(":wasmWasiNodeProductionLibraryDistribution")

                val dist = "build/dist/wasmWasi/productionLibrary"
                assertFileExists(projectPath.resolve("$dist/foo.txt"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library-wasm-wasi.wasm"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library-wasm-wasi.wasm.map"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library-wasm-wasi.mjs"))
            }
        }
    }


    @DisplayName("Browser print works with null type")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_6)
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

                val packageDir = "build/js/packages/mpp-wasm-js-browser-nodejs-wasm-js-test/kotlin"
                assertFileExists(projectPath.resolve("$packageDir/data.json"))
                assertFileExists(projectPath.resolve("$packageDir/data-test.json"))
            }
        }
    }

    @DisplayName("Test compilation package.json generation depends on main package.json generation")
    @GradleTest
    fun testPackageJsonDependsOnMainPackageJson(gradleVersion: GradleVersion) {
        project("mpp-wasm-js-browser-nodejs", gradleVersion) {
            build(":rootPackageJson") {
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

            build(":rootPackageJson") {
                assertTasksExecuted(
                    ":rootPackageJson"
                )

                val moduleDir = projectPath
                    .resolve("build/js/packages_imported/Kotlin-DateTime-library-kotlinx-datetime-wasm-js/0.6.0")

                val kotlinxDatetimePackageJson = moduleDir.resolve("package.json")

                assertFileExists(kotlinxDatetimePackageJson)
            }
        }
    }
}
