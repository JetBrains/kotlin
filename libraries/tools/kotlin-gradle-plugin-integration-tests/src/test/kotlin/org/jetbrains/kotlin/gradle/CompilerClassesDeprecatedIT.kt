/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Ensure that compiler classes bundled in KGP are deprecated")
@OtherGradlePluginTests
class CompilerClassesDeprecatedIT : KGPBaseTest() {
    @DisplayName("KT-70251: compiler class bundled into KGP is deprecated and reported by Gradle")
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED, maxVersion = TestVersions.Gradle.MAX_SUPPORTED)
    @GradleTest
    fun testCompilerClass(gradleVersion: GradleVersion) {
        // any project with Kotlin DSL is enough
        project("sourceSetsKotlinDsl", gradleVersion) {
            gradleProperties.append(
                """
                org.gradle.kotlin.dsl.allWarningsAsErrors=true
                """.trimIndent()
            )
            buildGradleKts.append(
                """
                println(org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION)
                """.trimIndent()
            )
            // if this test fails because KotlinCompilerVersion is not anymore accessible there,
            // replace it with any other accessible deprecated compiler class until KT-70247 is resolved
            // when KT-70247 is resolved, the test can be completely removed
            buildAndFail("help") {
                assertOutputContains("'KotlinCompilerVersion' is deprecated. You're using a Kotlin compiler class bundled into KGP for its internal needs.")
            }
        }
    }
}