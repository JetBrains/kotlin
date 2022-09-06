/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.sources

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.reflections
import kotlin.reflect.full.isSubclassOf
import kotlin.test.Test
import kotlin.test.fail

class InternalKotlinSourceSetTest {
    @Test
    fun `test - all implementations of KotlinSourceSet - implement InternalKotlinSourceSet`() {
        val subtypesOfKotlinSourceSet = reflections.getSubTypesOf(KotlinSourceSet::class.java)
        subtypesOfKotlinSourceSet
            .filter { subtype -> !subtype.isInterface }
            .forEach { implementation ->
                if (!implementation.kotlin.isSubclassOf(InternalKotlinSourceSet::class)) {
                    fail("$implementation does not implement ${InternalKotlinSourceSet::class}")
                }
            }
    }
}
