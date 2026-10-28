/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInWasm
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.readText
import kotlin.test.assertEquals

@WasmJsGradlePluginTests
@MustRunOnChangesInGradle
@MustRunOnChangesInWasm
class KotlinWasmBuiltinTasksGradlePluginIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Check js target dist")
    @GradleTest
    fun jsTargetDist(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "wasm-browser-simple-project"
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }

                    sourceSets.wasmJsMain.get().compileSource(
                        """     
                        fun main() {
                            println("Hello, world")
                        }
                        """.trimIndent()
                    )

                    sourceSets.wasmJsMain.dependencies {
                        implementation(npm("decamelize", "6.0.1"))
                    }
                }
            }

            build("build") {
                assertTasksExecuted(":wasmJsBrowserDistribution")

                val pathToDist = "build/dist/wasmJs/productionExecutable"
                assertDirectoryInProjectExists(pathToDist)
                assertFileInProjectExists("$pathToDist/wasm-browser-simple-project.mjs")
                assertFileInProjectExists("$pathToDist/wasm-browser-simple-project.wasm")

                assertFileInProjectExists("$pathToDist/importmap-loader.js")
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Check js target dist with npm dependency")
    @GradleTest
    fun jsTargetDistWithNpmDependencies(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "wasm-browser-simple-project"
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }

                    sourceSets.wasmJsMain.get().compileSource(
                        """     
                        fun main() {
                            println("Hello, world")
                        }
                        """.trimIndent()
                    )

                    sourceSets.wasmJsMain.dependencies {
                        implementation(npm("decamelize", "6.0.1"))
                    }
                }
            }

            build("build") {
                assertTasksExecuted(":wasmJsBrowserDistribution")

                val pathToDist = "build/dist/wasmJs/productionExecutable"
                assertDirectoryInProjectExists(pathToDist)
                assertFileInProjectExists("$pathToDist/wasm-browser-simple-project.mjs")
                assertFileInProjectExists("$pathToDist/wasm-browser-simple-project.wasm")

                assertFileInProjectExists("$pathToDist/importmap-loader.js")

                assertFileInProjectContains(
                    "$pathToDist/importmap-loader.js",
                    """"decamelize": "./vendors/decamelize/index.js""""
                )
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Check js target dist with npm dependency that has .bin")
    @GradleTest
    fun jsTargetDistWithNpmDependencyWithBin(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "wasm-browser-simple-project"
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }

                    sourceSets.wasmJsMain.get().compileSource(
                        """
                        fun main() {
                            println("Hello, world")
                        }
                        """.trimIndent()
                    )

                    sourceSets.wasmJsMain.dependencies {
                        npm("uuid", "11.1.0")
                    }
                }
            }

            build("build") {
                assertTasksExecuted(":wasmJsBrowserDistribution")

                assertDirectoryExists(projectPath.resolve("build/wasm/node_modules/.bin"))

                val imports = Json.decodeFromString<KotlinWebImportMap>(
                    projectPath.resolve("build/kotlin/wasmJsGenerateImportMapDist/importmap.json").readText()
                ).imports
                assertEquals(mapOf("uuid" to "./vendors/uuid/dist/esm/index.js"), imports)

                assertFileInProjectExists("build/dist/wasmJs/productionExecutable/vendors/uuid/dist/esm/index.js")
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @DisplayName("Check js target dist with nested npm dependencies")
    @GradleTest
    fun jsTargetDistWithNestedNpmDependencies(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "wasm-browser-simple-project"
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }

                    sourceSets.wasmJsMain.get().compileSource(
                        """     
                        fun main() {
                            println("Hello, world")
                        }
                        """.trimIndent()
                    )

                    sourceSets.wasmJsMain.dependencies {
                        // strip-ansi@6.0.1 is declared directly, so it is hoisted to the top level of
                        // node_modules. wrap-ansi@8.1.0 depends on strip-ansi@^7.0.1, which is incompatible
                        // with the hoisted 6.0.1, so npm cannot hoist it and nests it instead at
                        // node_modules/wrap-ansi/node_modules/strip-ansi (resolved to 7.2.0).
                        npm("strip-ansi", "6.0.1")
                        npm("wrap-ansi", "8.1.0")
                    }
                }
            }

            build("build") {
                assertTasksExecuted(":wasmJsBrowserDistribution")

                // sanity: npm really produced the nested copy, so the assertions below are not vacuous
                assertDirectoryInProjectExists("build/wasm/node_modules/wrap-ansi/node_modules/strip-ansi")

                val pathToDist = "build/dist/wasmJs/productionExecutable"
                assertFileInProjectExists("$pathToDist/vendors/strip-ansi/package.json")
                assertFileInProjectContains(
                    "$pathToDist/vendors/strip-ansi/package.json",
                    "\"version\": \"6.0.1\""
                )

                // nested dependency trees must not be copied into the distribution
                assertDirectoryInProjectExists("$pathToDist/vendors/wrap-ansi")
                assertDirectoryInProjectDoesNotExist("$pathToDist/vendors/wrap-ansi/node_modules")

                val imports = Json.decodeFromString<KotlinWebImportMap>(
                    projectPath.resolve("build/kotlin/wasmJsGenerateImportMapDist/importmap.json").readText()
                ).imports
                assertEquals("./vendors/strip-ansi/index.js", imports["strip-ansi"])
            }
        }
    }

    @Serializable
    private data class KotlinWebImportMap(
        val imports: Map<String, String>,
    )

}
