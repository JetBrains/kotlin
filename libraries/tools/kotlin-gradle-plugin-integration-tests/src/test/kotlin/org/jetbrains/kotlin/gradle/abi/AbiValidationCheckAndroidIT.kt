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
class AbiValidationCheckAndroidIT : KGPBaseTest() {
    @GradleAndroidTest
    fun testForDisabledAbiValidation(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidProject(gradleVersion, agpVersion, jdkVersion) {
            // skip lint as it shows the deprecation warning
            build("check", "-x", "lintDebug") {
                assertTasksAreNotInTaskGraph(":checkKotlinAbi")
            }
        }
    }

    @GradleAndroidTest
    fun testForEnabledAbiValidation(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        androidProject(gradleVersion, agpVersion, jdkVersion) {
            abiValidation()

            // create the reference dumps to check
            build("updateKotlinAbi")

            // skip lint as it shows the deprecation warning
            build("check", "-x", "lintDebug") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }
}