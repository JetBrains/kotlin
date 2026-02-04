/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.jvmProject
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.assertTasksAreNotInTaskGraph
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndAssertAllTasks

@JvmGradlePluginTests
class AbiValidationTasksIT : KGPBaseTest() {
    @GradleTest
    fun testForDisabledAbiValidation(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            buildAndAssertAllTasks(notRegisteredTasks = listOf(":checkKotlinAbi"))

            build("check") {
                assertTasksAreNotInTaskGraph(":checkKotlinAbi")
            }
        }
    }
}