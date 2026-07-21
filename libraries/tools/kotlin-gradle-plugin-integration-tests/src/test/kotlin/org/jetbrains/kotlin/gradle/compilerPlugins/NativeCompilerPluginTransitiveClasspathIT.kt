/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.compilerPlugins

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.OtherGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@DisplayName("Native compiler plugin transitive classpath")
@OtherGradlePluginTests
@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
class NativeCompilerPluginTransitiveClasspathIT : KGPBaseTest() {

    @DisplayName("Native compiler plugin classpath includes transitive dependencies")
    @GradleTest
    fun testNativeCompilerPluginTransitiveClasspath(
        gradleVersion: GradleVersion
    ) {
        project(
            "nativeCompilerPluginTransitiveClasspath",
            gradleVersion
        ) {
            buildAndFail(":app:compileKotlinNative") {
                assertOutputContains(
                    "ClassNotFoundException: test.helper.CompilerPluginHelper"
                )
            }
        }
    }
}
