/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@OtherGradlePluginTests
class KotlinxSerializationMppK2IT : KGPBaseTest() {
    private val gradleVersion = GradleVersion.version(TestVersions.Gradle.MAX_SUPPORTED)

    @DisplayName("Compile common code to metadata with kotlinx.serialization and K2")
    @GradleTest
    fun `test kotlinxSerializationMppK2`() {
        project("kotlinxSerializationMppK2", gradleVersion) {
            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
                assertOutputContains("BUILD SUCCESSFUL")
            }
        }
    }
}
