/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

internal class CompilerOptionsIT : KGPBaseTest() {

    // In Gradle 7.3-7.5 'kotlin-dsl' plugin tries to set up freeCompilerArgs in doFirst task action
    @DisplayName("Allows to set kotlinOptions.freeCompilerArgs on task execution with warning")
    @JvmGradlePluginTests
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_3,
        maxVersion = TestVersions.Gradle.G_7_5
    )
    @GradleTest
    internal fun compatibleWithKotlinDsl(gradleVersion: GradleVersion) {
        project("buildSrcWithKotlinDslAndKgp", gradleVersion) {
            build("tasks") {
                assertOutputContains("kotlinOptions.freeCompilerArgs were changed on task execution phase:")
            }
        }
    }
}