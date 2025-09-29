/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@MppGradlePluginTests
class KotlinWasmCustomRuntimeGradlePluginIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED)

    @DisplayName("Check wasi target with wasmtime")
    @GradleTest
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_8_0
    )
    @TestMetadata("new-mpp-wasm-wasi-custom-runtime")
    fun wasiWasmEdgeTarget(gradleVersion: GradleVersion) {
        testWithCustomRuntime("wasmtime", gradleVersion)
    }

    private fun testWithCustomRuntime(runtime: String, gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-custom-runtime", gradleVersion) {
            settingsGradleKts.modify {
                it.replace(
                    "RepositoriesMode.PREFER_SETTINGS",
                    "RepositoriesMode.PREFER_PROJECT"
                )
            }

            subProject("buildSrc").settingsGradleKts.modify {
                it.replace(
                    "RepositoriesMode.PREFER_SETTINGS",
                    "RepositoriesMode.PREFER_PROJECT"
                )
            }

            buildGradleKts.appendText(
                """
                kotlin {
                    wasmWasi {
                        ${runtime}()
                    }
                }
            """.trimIndent()
            )

            val capitalizedRuntime = runtime.capitalizeAsciiOnly()
            build("build") {
                assertTasksExecuted(":kotlin${capitalizedRuntime}Setup")
                assertTasksExecuted(":wasmWasi${capitalizedRuntime}Test")
            }

            build(":wasmWasi${capitalizedRuntime}DevelopmentRun") {
                assertTasksUpToDate(":kotlin${capitalizedRuntime}Setup")
                assertTasksExecuted(":wasmWasi${capitalizedRuntime}DevelopmentRun")
            }
        }
    }
}
