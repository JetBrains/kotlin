/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmWithJavaTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.junit.Test
import kotlin.test.fail

class KotlinHierarchyBuilderTest {

    @Test
    fun `test - interface offers functions for known presets`() {
        val kotlinTargetHierarchyBuilderInterface = KotlinHierarchyBuilder::class.java

        @Suppress("DEPRECATION_ERROR")
        buildProjectWithMPP().multiplatformExtension.presetFunctions.presets

            // JS targets are special and therefore are only handled manually using `withJs()`
            .filter { it !is KotlinJsIrTargetPreset }
            .filter { it !is KotlinWasmTargetPreset }

            // jvmWithJava is covered by the withJvm() call
            .filter { it !is KotlinJvmWithJavaTargetPreset }
            .filter { it.name != "linuxArm32Hfp" } // KT-61122. Deprecated target. We do not support it in the hierarchy builder
            .forEach { preset ->
                val presetName = if (preset.name == "android") "androidTarget" else preset.name
                val expectedFunctionName = "with${presetName.capitalizeAsciiOnly()}"
                if (kotlinTargetHierarchyBuilderInterface.declaredMethods.none { it.name == expectedFunctionName })
                    fail("${kotlinTargetHierarchyBuilderInterface.name}: Missing ${expectedFunctionName}() function")
            }
    }
}
