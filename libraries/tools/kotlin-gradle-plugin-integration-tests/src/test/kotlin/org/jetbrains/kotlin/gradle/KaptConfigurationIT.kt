/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Generic Kapt configuration")
@OtherGradlePluginTests
class KaptConfigurationIT : KGPBaseTest() {

    @DisplayName("KT-55452: KaptGenerateStubs task compiler options are not duplicated")
    @GradleTest
    @TestMetadata("kapt2/simple")
    fun testKaptGenerateStubsCompilerOptionsDup(gradleVersion: GradleVersion) {
        project(
            "kapt2/simple",
            gradleVersion,
        ) {
            buildGradle.appendText(
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                |    compilerOptions {
                |        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
                |        freeCompilerArgs.addAll([
                |            "-P",
                |            "plugin:androidx.compose.compiler.plugins.kotlin:intrinsicRemember=true",
                |            "-P",
                |            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                |            project.buildDir.absolutePath + "/compose_metrics"
                |        ])
                |    }
                |}
                |
                """.trimMargin()
            )

            build(":compileKotlin") {
                val compilerArguments = extractTaskCompilerArguments(":kaptGenerateStubsKotlin")
                    .split(" ")

                val pOption = compilerArguments.filter { it == "-P" }.size
                // 2 from freeArgs and 1 for kapt itself
                assert(pOption <= 3) {
                    printBuildOutput()
                    "KaptGenerateStubs task compiler arguments contains $pOption times '-P' option: ${compilerArguments.joinToString("\n")}"
                }

                val composeSuppressOption = compilerArguments
                    .filter {
                        it == "plugin:androidx.compose.compiler.plugins.kotlin:intrinsicRemember=true"
                    }
                    .size
                assert(composeSuppressOption == 1) {
                    printBuildOutput()
                    "KaptGenerateStubs task compiler arguments contains $composeSuppressOption times option to suppress compose warning:" +
                            " ${compilerArguments.joinToString("\n")}"
                }
            }
        }
    }
}