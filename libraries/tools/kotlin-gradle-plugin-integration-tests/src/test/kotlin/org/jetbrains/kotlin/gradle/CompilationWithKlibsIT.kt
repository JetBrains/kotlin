/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Klib compilation")
class CompilationWithKlibsIT : KGPBaseTest() {

    @DisplayName("KT-64115: duplicated klibs are excluded in the JS test compilations")
    @JsGradlePluginTests
    @GradleTest
    @TestMetadata("kt-64115")
    fun testDuplicatedKlibsInJsTestCompilation(
        gradleVersion: GradleVersion,
    ) {
        project(
            "kt-64115",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
        ) {
            build(":foo:compileTestKotlinJs") {
                assertTasksExecuted(":foo:compileTestKotlinJs")
                assertOutputDoesNotContain("w: KLIB resolver: The same 'unique_name=kt-64115:foo' found in more than one library:")
                assertNoCompilerArgument(
                    ":foo:compileTestKotlinJs",
                    "libs/foo-js.klib",
                    logLevel = LogLevel.INFO
                )
            }
        }
    }
}
