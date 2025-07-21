/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*

@MppGradlePluginTests
class WasmMultimoduleIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED)

    @GradleTest
    fun `test wasm multimodule`(version: GradleVersion) {
        project("wasm-d8-multimodule", version) {
            // Compile the project in master mode
            build("wasmJsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":wasmJsDevelopmentExecutableCompileSync")
            }

            // Saving the output to the separate directory
            build("masterCopy") {
                assertTasksExecuted(":masterCopy")
            }

            // Disable master mode
            buildGradleKts.modify {
                it.replace("MULTIMODULE_MODE_MASTER_ENABLE", "")
            }

            // Compile project in slave mode
            build("wasmJsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":wasmJsDevelopmentExecutableCompileSync")
            }

            // Backing up master module compilation
            build("masterCopyBack") {
                assertTasksExecuted(":masterCopyBack")
            }

            // Run the multimodule project
            build("wasmJsD8DevelopmentRun") {
                assertTasksExecuted(":wasmJsD8DevelopmentRun")
                assertOutputContains("Multimodule is FINE!")
            }
        }
    }
}
