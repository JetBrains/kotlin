/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@MppGradlePluginTests
@DisplayName("Tests for JVM target version attribute in KMP projects")
class JvmTargetAttributeIT : KGPBaseTest() {
    @GradleTest
    @DisplayName("JVM target is configured lazily")
    fun testLaziness(gradleVersion: GradleVersion) {
        project("new-mpp-jvm-with-java-multi-module", gradleVersion) {
            subProject("lib").buildGradle.appendText(
                """

                project.afterEvaluate {
                    tasks.getByName("compileJava").targetCompatibility = JavaVersion.VERSION_17
                    tasks.getByName("compileKotlinJvm").compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
                }
                """.trimIndent()
            )
            subProject("app").buildGradle.appendText(
                """

                project.afterEvaluate {
                    tasks.getByName("compileKotlinJvm").compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
                }
                """.trimIndent()
            )
            buildAndFail(":app:compileKotlinJvm") {
                assertOutputContains(
                    "Incompatible because this component declares a component,? compatible with Java 17 and the consumer needed a component,? compatible with Java 11".toRegex()
                )
            }
        }
    }
}