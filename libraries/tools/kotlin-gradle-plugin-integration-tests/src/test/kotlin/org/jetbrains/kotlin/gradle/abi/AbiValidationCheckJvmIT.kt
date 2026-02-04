/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.*
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*

@JvmGradlePluginTests
class AbiValidationCheckJvmIT : KGPBaseTest() {
    @GradleTest
    fun testForDisabledAbiValidation(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            build("check") {
                assertTasksAreNotInTaskGraph(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    fun testForEnabledAbiValidation(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation {
                enabled.set(true)
            }
            // create the reference dumps to check
            build("updateKotlinAbi")

            build("check") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }
}