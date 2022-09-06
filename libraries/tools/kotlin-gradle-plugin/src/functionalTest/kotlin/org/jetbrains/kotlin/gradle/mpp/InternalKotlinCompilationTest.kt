/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.reflections
import org.junit.Test
import kotlin.reflect.full.isSubclassOf
import kotlin.test.fail

class InternalKotlinSourceSetTest {
    @Test
    fun `test - all implementations of KotlinCompilation - implement InternalKotlinCompilation`() {
        val subtypesOfKotlinSourceSet = reflections.getSubTypesOf(KotlinCompilation::class.java)
        subtypesOfKotlinSourceSet
            .filter { subtype -> !subtype.isInterface }
            .forEach { implementation ->
                if (!implementation.kotlin.isSubclassOf(InternalKotlinCompilation::class)) {
                    fail("$implementation does not implement ${InternalKotlinCompilation::class}")
                }
            }
    }
}
