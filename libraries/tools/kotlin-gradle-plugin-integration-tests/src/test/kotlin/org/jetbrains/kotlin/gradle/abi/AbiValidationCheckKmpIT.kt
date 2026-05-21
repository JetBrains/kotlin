/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.abi.utils.abiValidation
import org.jetbrains.kotlin.gradle.abi.utils.kmpProject
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@MppGradlePluginTests
class AbiValidationCheckKmpIT : KGPBaseTest() {
    @GradleTest
    fun testForEnabledAbiValidation(
        gradleVersion: GradleVersion,
    ) {
        kmpProject(gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
            }

            abiValidation()

            // create the reference dumps to check
            build("updateKotlinAbi")

            build("check") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    @GradleTest
    @DisplayName("Test abiValidation compatibility with compilerVersion prior 2.4.0")
    fun testCompilerVersionCompatibility(gradleVersion: GradleVersion) {
        kmpProject(gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.compilerVersion.set("2.2.0")
            }

            abiValidation()

            build("updateKotlinAbi")

            build("check") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }
}
