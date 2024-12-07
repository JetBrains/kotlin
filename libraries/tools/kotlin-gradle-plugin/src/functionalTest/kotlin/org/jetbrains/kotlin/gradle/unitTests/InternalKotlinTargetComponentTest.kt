/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTargetComponent
import org.jetbrains.kotlin.gradle.util.assertAllImplementationsAlsoImplement
import kotlin.test.Test

class InternalKotlinTargetComponentTest {
    @Test
    fun `test - all implementations of KotlinTargetComponent - implement InternalKotlinTargetComponent`() {
        assertAllImplementationsAlsoImplement(KotlinTargetComponent::class, InternalKotlinTargetComponent::class)
    }
}
