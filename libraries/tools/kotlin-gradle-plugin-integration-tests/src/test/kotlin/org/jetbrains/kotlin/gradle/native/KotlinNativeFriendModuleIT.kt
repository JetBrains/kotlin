/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.fail

@DisplayName("Tests for friendModule compiler argument in KMP native compilations")
@NativeGradlePluginTests
class KotlinNativeFriendModuleIT : KGPBaseTest() {

    @GradleTest
    @DisplayName("Compiling a native test source set with only test sources reports no compiler warnings")
    fun testCompileNativeWithOnlyTestSourceSet(gradleVersion: GradleVersion) {
        nativeProject("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxX64()
                    // Only the test source set has sources; the main source set is empty.
                    sourceSets.linuxX64Test.get().compileStubSourceWithSourceSetName()
                }
            }

            build(":compileTestKotlinLinuxX64") {
                assertTasksExecuted(":compileTestKotlinLinuxX64")
                val args = extractNativeCompilerTaskArguments(":compileTestKotlinLinuxX64")
                assertNoBuildWarnings()
                if (args.contains("-friend-modules"))
                    fail(
                        """
                            Unexpected -friend-modules argument found in compileTestKotlinLinuxX64 task arguments. 
                            No main source set added -> no main klib created -> friend modules should not declare it
                        """.trimIndent()
                    )
            }
        }
    }
}
