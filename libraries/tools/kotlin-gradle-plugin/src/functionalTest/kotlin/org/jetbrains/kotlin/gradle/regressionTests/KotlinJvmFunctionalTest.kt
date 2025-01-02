/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.targets
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertNull

class KotlinJvmFunctionalTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test
    fun `test setting dependencies via compilation dependency handler`() {
        val project = buildProjectWithJvm {
            kotlinExtension.apply {
                val jvmTarget = this.targets.singleOrNull() ?: error("Expected single target for Kotlin JVM extension")

                jvmTarget.compilations.getByName("main").dependencies {
                    api(files())
                    implementation(files())
                    compileOnly(files())
                    runtimeOnly(files())
                }

                jvmTarget.compilations.getByName("test").dependencies {
                    api(files())
                    implementation(files())
                    compileOnly(files())
                    runtimeOnly(files())
                }
            }
        }

        project.evaluate()
    }

    @Test
    fun `KT-66750 - check that disabled native toolchain flag in subproject does not affect root project`() {
        val project = buildProjectWithJvm(preApplyCode = {
            project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "true")
            project.extraProperties.set("kotlin.native.toolchain.enabled", "true")
        })

        project.evaluate()

        val kotlinNativeConfiguration = project.configurations.findByName(KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME)
        assertNull(kotlinNativeConfiguration, "Kotlin Native bundle configuration should not be created")
    }
}
