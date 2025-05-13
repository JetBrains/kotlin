/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertNull

class KT71398KotlinNativeBundleConfigurationOnUnsupportedPlatform {

    @Test
    fun `KT-71398 - project with multiplatform plugin should not add kotlinNativeBundleConfiguration`() {
        Assume.assumeTrue(!HostManager.hostIsMac)
        val project = buildProjectWithMPP(preApplyCode = {
            project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "true")
            project.extraProperties.set("kotlin.native.toolchain.enabled", "true")
            project.extraProperties.set("kotlin.native.disableKlibsCrossCompilation", "true")
        }) {
            val kotlin = project.multiplatformExtension
            kotlin.macosArm64()
        }

        project.evaluate()

        val kotlinNativeConfiguration = project.configurations.findByName(KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME)
        assertNull(
            kotlinNativeConfiguration,
            "Kotlin Native bundle configuration should not be created on current platform for current target"
        )
    }
}