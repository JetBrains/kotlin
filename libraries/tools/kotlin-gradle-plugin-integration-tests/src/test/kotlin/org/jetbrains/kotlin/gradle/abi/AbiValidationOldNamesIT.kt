/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.abiValidation
import org.jetbrains.kotlin.gradle.abi.utils.androidProject
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*

@AndroidGradlePluginTests
class AbiValidationOldNamesIT : KGPBaseTest() {
    @GradleAndroidTest
    fun testOldTasksAreExecuted(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()

            build("updateLegacyAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            build("checkLegacyAbi") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

}