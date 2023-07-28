/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.util.assertAllImplementationsAlsoImplement
import org.junit.Test

@Suppress("FunctionName")
class InternalKotlinTargetPresetTest {
    @Test
    fun `test - all implementations of KotlinTargetPreset - implement InternalKotlinTargetPreset`() {
        assertAllImplementationsAlsoImplement(KotlinTargetPreset::class, InternalKotlinTargetPreset::class)
    }
}