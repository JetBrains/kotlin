/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget
import org.jetbrains.kotlin.gradle.util.assertAllImplementationsAlsoImplement
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class InternalKotlinTargetTest {
    @Test
    fun `test - all implementations of KotlinTarget - implement InternalKotlinTarget`() {
        assertAllImplementationsAlsoImplement(KotlinTarget::class, InternalKotlinTarget::class)
    }

    @Test
    fun `KotlinTarget sourceSets are equal to extension sourceSets`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
            }
        }

        project.evaluate()

        @Suppress("DEPRECATION")
        assertEquals(
            project.multiplatformExtension.sourceSets,
            project.multiplatformExtension.jvm().sourceSets
        )
    }
}