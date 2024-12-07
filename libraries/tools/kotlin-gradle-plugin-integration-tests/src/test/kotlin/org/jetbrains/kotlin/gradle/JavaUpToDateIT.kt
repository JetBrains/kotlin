/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Changes in Kotlin/Java sources are correctly recoginized")
@JvmGradlePluginTests
class JavaUpToDateIT : KGPBaseTest() {

    @DisplayName("On Kotlin method body change")
    @GradleTest
    fun testKotlinMethodBodyIsChanged(gradleVersion: GradleVersion) {
        project("javaUpToDate", gradleVersion) {
            build("testClasses") {
                assertTasksExecuted(
                    ":compileKotlin",
                    ":compileJava",
                    ":compileTestKotlin",
                    ":compileTestJava"
                )
            }

            kotlinSourcesDir().resolve("foo/MainKotlinClass.kt").modify {
                it.replace(
                    "fun number(): Int = 0",
                    "fun number(): Int = 1"
                )
            }

            build("testClasses") {
                assertTasksExecuted(":compileKotlin")
                assertTasksUpToDate(":compileJava", ":compileTestJava", ":compileTestKotlin")
            }
        }
    }

    @DisplayName("On Kotlin source new newline")
    @GradleTest
    fun testKotlinNewLineAdded(gradleVersion: GradleVersion) {
        project("javaUpToDate", gradleVersion) {
            build("testClasses") {
                assertTasksExecuted(
                    ":compileKotlin",
                    ":compileJava",
                    ":compileTestKotlin",
                    ":compileTestJava"
                )
            }

            kotlinSourcesDir().resolve("foo/MainKotlinClass.kt").modify { "\n$it" }

            build("testClasses") {
                assertTasksExecuted(":compileKotlin")
                assertTasksUpToDate(":compileJava", ":compileTestJava", ":compileTestKotlin")
            }
        }
    }

    @DisplayName("On Kotlin private method return type change")
    @GradleTest
    fun testPrivateMethodSignatureChanged(gradleVersion: GradleVersion) {
        project("javaUpToDate", gradleVersion) {
            build("testClasses") {
                assertTasksExecuted(
                    ":compileKotlin",
                    ":compileJava",
                    ":compileTestKotlin",
                    ":compileTestJava"
                )
            }

            kotlinSourcesDir().resolve("foo/MainKotlinClass.kt").modify {
                it.replace(
                    "private fun privateMethod() = 0",
                    "private fun privateMethod() = \"0\""
                )
            }

            build("testClasses") {
                // see https://github.com/gradle/gradle/issues/5013
                assertTasksExecuted(":compileKotlin", ":compileJava", ":compileTestKotlin", ":compileTestJava")
            }
        }
    }
}