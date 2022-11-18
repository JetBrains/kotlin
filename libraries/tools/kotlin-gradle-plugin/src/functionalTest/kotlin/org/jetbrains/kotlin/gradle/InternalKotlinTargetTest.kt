/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget
import kotlin.test.Test

class InternalKotlinTargetTest {
    @Test
    fun `test - all implementations of KotlinTarget - implement InternalKotlinTarget`() {
        assertAllImplementationsAlsoImplement(KotlinTarget::class, InternalKotlinTarget::class)
    }
}