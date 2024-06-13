/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.deleteIfExists
import kotlin.io.path.moveTo

@MppGradlePluginTests
class MppDslLibWithTestsIt : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-with-tests")
    fun testLibWithTests(gradleVersion: GradleVersion) {
        nativeProject("new-mpp-lib-with-tests", gradleVersion) {
            doTestLibWithTests()
        }
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-with-tests")
    fun testLibWithTestsKotlinDsl(gradleVersion: GradleVersion) {
        nativeProject("new-mpp-lib-with-tests", gradleVersion) {
            buildGradle.deleteIfExists()
            projectPath.resolve("alternative.build.gradle.kts").moveTo(buildGradleKts)
            doTestLibWithTests()
        }
    }

    private fun TestProject.doTestLibWithTests() {
        build("check") {
            assertTasksExecuted(
                // compilation tasks:
                ":compileKotlinJs",
                ":compileTestKotlinJs",
                ":compileKotlinJvmWithoutJava",
                ":compileTestKotlinJvmWithoutJava",
                // test tasks:
                ":jsTest", // does not run any actual tests for now
                ":jvmWithoutJavaTest",
            )

            val mainClassesDir = projectPath.resolve("build/classes/kotlin/jvmWithoutJava/main/")
            val testClassesDir = projectPath.resolve("build/classes/kotlin/jvmWithoutJava/test/")

            val expectedKotlinOutputFiles = listOf(
                mainClassesDir.resolve("com/example/lib/CommonKt.class"),
                mainClassesDir.resolve("com/example/lib/MainKt.class"),
                mainClassesDir.resolve("META-INF/new-mpp-lib-with-tests.kotlin_module"),

                testClassesDir.resolve("com/example/lib/TestCommonCode.class"),
                testClassesDir.resolve("com/example/lib/TestWithoutJava.class"),
                testClassesDir.resolve("META-INF/new-mpp-lib-with-tests_test.kotlin_module"),
            )

            expectedKotlinOutputFiles.forEach { assertFileExists(it) }

            val expectedTestResults = projectPath.resolve("TEST-all.xml")

            val currentTarget = MPPNativeTargets.current
            expectedTestResults.replaceText("<target>", currentTarget)

            assertTestResults(
                expectedTestResults,
                "jsNodeTest",
                "${currentTarget}Test"
            )
        }
    }
}
