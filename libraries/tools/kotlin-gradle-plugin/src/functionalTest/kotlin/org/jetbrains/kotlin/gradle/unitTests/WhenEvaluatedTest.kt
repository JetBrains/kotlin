/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import com.android.build.api.variant.AndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.getByType
import org.jetbrains.kotlin.gradle.utils.whenEvaluated
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFalse

class WhenEvaluatedTest {
    @Test
    fun `test - whenEvaluated - waits for Android plugins afterEvaluate`() {
        val project = buildProject()
        project.androidLibrary { compileSdk = 33 }
        project.applyMultiplatformPlugin()
        project.multiplatformExtension.androidTarget()

        val onVariantsCalled = AtomicBoolean(false)
        val onWhenEvaluatedCalled = AtomicBoolean(false)

        project.extensions.getByType<AndroidComponentsExtension<*, *, *>>().onVariants {
            assertFalse(onVariantsCalled.getAndSet(true))
            assertFalse(onWhenEvaluatedCalled.get(), "whenEvaluated was called before 'onVariants'")
        }

        project.whenEvaluated {
            assertFalse(onVariantsCalled.get(), "whenEvaluated was called after 'onVariants'")
            assertFalse(onWhenEvaluatedCalled.getAndSet(true))
        }
    }
}