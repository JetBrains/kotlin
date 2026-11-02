/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.maxParallelUsagesOf
import org.jetbrains.kotlin.internal.compilerRunner.native.registerKotlinNativeParallelismLimitService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class KotlinNativeToolParallelismLimitServiceTest {

    @Test
    fun `registerKotlinNativeParallelismLimitService with an int sets that value as maxParallelUsages`() {
        val project = buildProject()

        project.registerKotlinNativeParallelismLimitService("SomeTask", maxParallelUsages = 3)

        assertEquals(3, project.maxParallelUsagesOf("SomeTask"))
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService reuses the same service instance for the same name`() {
        val project = buildProject()

        val first = project.registerKotlinNativeParallelismLimitService("SomeTask", maxParallelUsages = 2)
        val second = project.registerKotlinNativeParallelismLimitService("SomeTask", maxParallelUsages = 2)

        assertSame(first.get(), second.get())
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService uses independent services for different names`() {
        val project = buildProject()

        project.registerKotlinNativeParallelismLimitService("FirstTask", maxParallelUsages = 2)
        project.registerKotlinNativeParallelismLimitService("SecondTask", maxParallelUsages = 6)

        assertEquals(2, project.maxParallelUsagesOf("FirstTask"))
        assertEquals(6, project.maxParallelUsagesOf("SecondTask"))
    }
}
