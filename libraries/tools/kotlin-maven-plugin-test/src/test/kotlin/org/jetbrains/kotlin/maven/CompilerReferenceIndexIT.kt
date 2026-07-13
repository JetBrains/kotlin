/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Compiler reference index")
class CompilerReferenceIndexIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Warns when CRI generation is enabled but incremental compilation is disabled")
    fun testCriRequiresIncrementalCompilation(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.generateCompilerRefIndex", "true")
            setMavenProperty("kotlin.compiler.incremental", "false")

            build("compile") {
                assertBuildLogContains("Compiler reference index generation requires incremental compilation")
            }
        }
    }

    @MavenTest
    @DisplayName("Does not warn when CRI generation and incremental compilation are enabled")
    fun testCriWithIncrementalCompilationEnabled(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.generateCompilerRefIndex", "true")
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile") {
                assertBuildLogContains("Using experimental Kotlin incremental compilation")
                assertBuildLogDoesNotContain("Compiler reference index generation requires incremental compilation")
            }
        }
    }
}
