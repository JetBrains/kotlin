/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.native.MPPNativeTargets
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
@DisplayName("Tests for multiplatform testing")
class MppTestsIT : MPPBaseTest() {
    @DisplayName("KT-54634: MPP testing logic is compatible with API changes in Gradle 7.6")
    @GradleTestVersions(
        maxVersion = BETA_GRADLE,
        additionalVersions = [TestVersions.Gradle.G_7_5, TestVersions.Gradle.G_7_6]
    )
    @GradleTest
    fun testKt54634(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                freeArgs = listOf("--continue"), // to ensure that all the tests are run
            )
        ) {
            val nativeTarget = MPPNativeTargets.current

            build(":allTests") {
                assertTasksExecuted(
                    ":jsNodeTest",
                    ":jvmWithoutJavaTest",
                    ":${nativeTarget}Test"
                )
            }

            // break all the test tasks
            kotlinSourcesDir("commonTest").resolve("TestCommonCode.kt").modify {
                it.replace("expectedFun()", "assertEquals(0, 1)")
            }
            buildAndFail(":allTests") {
                assertTasksFailed(
                    ":jsNodeTest",
                    ":jvmWithoutJavaTest",
                    ":${nativeTarget}Test"
                )
                assertOutputDoesNotContain("does not define or inherit an implementation of the resolved method")
                assertOutputDoesNotContain("NoSuchMethodError")
                assertOutputContainsExactlyTimes("AssertionError:", 3)
            }
        }
    }
}