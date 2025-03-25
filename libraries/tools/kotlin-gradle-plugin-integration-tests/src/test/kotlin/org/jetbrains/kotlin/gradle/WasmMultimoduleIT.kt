/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.compilerRunner.getCompilerArgumentImplementations
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.io.path.appendText

@MppGradlePluginTests
class WasmMultimoduleIT : KGPBaseTest() {
    @GradleTest
    fun `test wasm multimodule`(version: GradleVersion) {
        project("wasm-d8-multimodule", version) {
            // Compile project in master mode
            build("wasmJsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":wasmJsDevelopmentExecutableCompileSync")
            }

            // Saving the output to separate directory
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

            // Run multimodule project
            build("wasmJsD8DevelopmentRun") {
                assertTasksExecuted(":wasmJsD8DevelopmentRun")
                assertOutputContains("Multimodule is FINE!")
            }
        }
    }
}
