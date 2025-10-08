/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test

class CrossCompilationPublishingTests {

    @Test
    fun `test KT-81134 Gradle configuration failure when accessing publishable property`() {
        with(buildProjectWithMPP {
            kotlin {
                macosX64()
                linuxX64()
                mingwX64()
            }
        }) {
            multiplatformExtension.targets.withType(KotlinNativeTarget::class.java).configureEach { nativeTarget ->
                nativeTarget.publishable
            }

            evaluate()
        }
    }
}