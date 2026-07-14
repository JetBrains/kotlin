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
    @DisplayName("Warns and skips CRI generation when incremental compilation is disabled")
    fun testCriRequiresIncrementalCompilation(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.generateCompilerRefIndex", "true")
            setMavenProperty("kotlin.compiler.incremental", "false")

            build("compile") {
                assertBuildLogContains("Compiler reference index generation requires incremental compilation")
                assertFileNotExists("target/kotlin-ic/compile/cri")
            }
        }
    }

    @MavenTest
    @DisplayName("Generates CRI without warning when incremental compilation is enabled")
    fun testCriWithIncrementalCompilationEnabled(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            setMavenProperty("kotlin.compiler.generateCompilerRefIndex", "true")
            setMavenProperty("kotlin.compiler.incremental", "true")

            build("compile") {
                assertBuildLogContains("Using experimental Kotlin incremental compilation")
                assertBuildLogDoesNotContain("Compiler reference index generation requires incremental compilation")
                assertFileExists("target/kotlin-ic/compile/cri/subtypes.table")
            }
        }
    }
}
