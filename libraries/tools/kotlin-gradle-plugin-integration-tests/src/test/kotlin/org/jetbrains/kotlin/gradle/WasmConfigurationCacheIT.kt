/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@MppGradlePluginTests
class WasmConfigurationCacheIT : KGPBaseTest() {
    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
        )

    @DisplayName("configuration cache is working for wasm")
    @GradleTest
    fun testKotlinWasmCompilation(gradleVersion: GradleVersion) {
        project("wasm-d8-simple-project", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                "assemble",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(":compileKotlinWasmJs")
            )
        }
    }

    @DisplayName("D8 run correctly works with configuration cache")
    @GradleTest
    fun testD8Run(gradleVersion: GradleVersion) {
        project(
            "wasm-d8-simple-project",
            gradleVersion,
            dependencyManagement = DependencyManagement.DisabledDependencyManagement // :d8Download adds custom ivy repository during build
        ) {
            build("wasmJsD8DevelopmentRun", buildOptions = buildOptions) {
                assertTasksExecuted(":wasmJsD8DevelopmentRun")
                if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_5)) {
                    assertOutputContains(
                        "Calculating task graph as no configuration cache is available for tasks: wasmJsD8DevelopmentRun"
                    )
                } else {
                    assertOutputContains(
                        "Calculating task graph as no cached configuration is available for tasks: wasmJsD8DevelopmentRun"
                    )
                }

                assertConfigurationCacheStored()
            }

            build("clean", buildOptions = buildOptions)

            // Then run a build where tasks states are deserialized to check that they work correctly in this mode
            build("wasmJsD8DevelopmentRun", buildOptions = buildOptions) {
                assertTasksExecuted(":wasmJsD8DevelopmentRun")
                assertConfigurationCacheReused()
            }
        }
    }

    @DisplayName("Browser case works correctly with configuration cache")
    @GradleTest
    fun testBrowser(gradleVersion: GradleVersion) {
        project("wasm-browser-simple-project", gradleVersion) {
            assertSimpleConfigurationCacheScenarioWorks(
                "assemble",
                buildOptions = defaultBuildOptions,
                executedTaskNames = listOf(":compileKotlinWasmJs", ":wasmJsBrowserDistribution")
            )
        }
    }
}
