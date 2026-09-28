/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
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

}
