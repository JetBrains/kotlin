/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
class KotlinWasmBuiltinTasksGradlePluginIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("Check js target dist")
    @GradleTest
    @TestMetadata("wasm-browser-simple-project")
    fun jsTargetDist(gradleVersion: GradleVersion) {
        project("wasm-browser-simple-project", gradleVersion) {
            buildScriptInjection {
                @OptIn(ExperimentalWasmDsl::class)
                kotlinMultiplatform.wasmJs {
                    browser {
                        bundler.set(org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserBundler.NONE)
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

    @DisplayName("Check js target dist with npm dependency")
    @GradleTest
    @TestMetadata("wasm-browser-simple-project")
    fun jsTargetDistWithNpmDependencies(gradleVersion: GradleVersion) {
        project("wasm-browser-simple-project", gradleVersion) {
            buildScriptInjection {
                @OptIn(ExperimentalWasmDsl::class)
                kotlinMultiplatform.wasmJs {
                    browser {
                        bundler.set(org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserBundler.NONE)
                    }
                }
                kotlinMultiplatform.sourceSets.getByName("wasmJsMain").dependencies {
                    implementation(npm("decamelize", "6.0.1"))
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
