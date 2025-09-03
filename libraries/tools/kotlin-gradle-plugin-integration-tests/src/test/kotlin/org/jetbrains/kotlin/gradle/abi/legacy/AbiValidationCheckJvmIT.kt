/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi.legacy

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.*
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*

@JvmGradlePluginTests
class AbiValidationCheckJvmIT : KGPBaseTest() {
    @GradleTest
    fun testForDisabledAbiValidationWithVariants(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation<AbiValidationExtension> {
                variants.create("custom")
            }
            build("check") {
                assertTasksAreNotInTaskGraph(":checkLegacyAbi")
                assertTasksAreNotInTaskGraph(":checkLegacyAbiCustom")
            }
        }
    }

    @GradleTest
    fun testForEnabledAbiValidationWithVariants(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation<AbiValidationExtension> {
                enabled.set(true)
                variants.create("custom")
            }
            // create the reference dumps to check
            build("updateLegacyAbi")
            build("updateLegacyAbiCustom")

            build("check") {
                assertTasksExecuted(":checkLegacyAbi")
                assertTasksExecuted(":checkLegacyAbiCustom")
            }
        }
    }
}