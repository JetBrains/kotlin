/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.maxParallelUsagesOf
import org.jetbrains.kotlin.gradle.util.withGradleProperty
import org.jetbrains.kotlin.internal.compilerRunner.native.maxParallelUsagesFromAvailableRam
import org.jetbrains.kotlin.internal.compilerRunner.native.maxParallelUsagesFromGradleProperty
import org.jetbrains.kotlin.internal.compilerRunner.native.registerKotlinNativeParallelismLimitService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KotlinNativeToolParallelismLimitServiceTest {

    @Test
    fun `maxParallelUsagesFromGradleProperty returns the parsed value when the property is a valid int`() {
        withGradleProperty("some.property", value = "7") {
            val project = buildProject()

            val strategy = maxParallelUsagesFromGradleProperty(project.providers, "some.property")

            assertEquals(7, strategy.calculateMaxParallelUsages())
        }
    }

    @Test
    fun `maxParallelUsagesFromGradleProperty returns null when the property is not set`() {
        withGradleProperty("some.property", value = null) {
            val project = buildProject()

            val strategy = maxParallelUsagesFromGradleProperty(project.providers, "some.property")

            assertNull(strategy.calculateMaxParallelUsages())
        }
    }

    @Test
    fun `maxParallelUsagesFromGradleProperty returns null when the property is not a valid int`() {
        withGradleProperty("some.property", value = "not-an-int") {
            val project = buildProject()

            val strategy = maxParallelUsagesFromGradleProperty(project.providers, "some.property")

            assertNull(strategy.calculateMaxParallelUsages())
        }
    }

    @Test
    fun `maxParallelUsagesFromAvailableRam calculates how many instances fit into the available daemon memory`() {
        val strategy = maxParallelUsagesFromAvailableRam(approximateTaskInstanceRamUsageMb = 1L)

        val maxParallelUsages = strategy.calculateMaxParallelUsages()

        assertTrue(maxParallelUsages!! > 1, "Expected maxParallelUsages > 1, but was $maxParallelUsages")
    }

    @Test
    fun `maxParallelUsagesFromAvailableRam returns at least 1 instance even when it would exceed the available memory`() {
        val hugeTaskRamUsageMb = 16_000L

        val strategy = maxParallelUsagesFromAvailableRam(hugeTaskRamUsageMb)

        assertEquals(1, strategy.calculateMaxParallelUsages())
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService with an int sets that value as maxParallelUsages`() {
        val project = buildProject()

        project.registerKotlinNativeParallelismLimitService("SomeTask", maxParallelUsages = 3)

        assertEquals(3, project.maxParallelUsagesOf("SomeTask"))
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService with a strategy uses the value it returns`() {
        val project = buildProject()

        project.registerKotlinNativeParallelismLimitService("SomeTask", maxParallelUsagesCalculatorStrategy = { 5 })

        assertEquals(5, project.maxParallelUsagesOf("SomeTask"))
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService with a strategy returning null sets no limit`() {
        val project = buildProject()

        project.registerKotlinNativeParallelismLimitService("SomeTask", maxParallelUsagesCalculatorStrategy = { null })

        assertNull(project.maxParallelUsagesOf("SomeTask"))
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService falls back to the first fallback strategy returning a value`() {
        val project = buildProject()

        project.registerKotlinNativeParallelismLimitService(
            "SomeTask",
            maxParallelUsagesCalculatorStrategy = { null },
            { null },
            { 4 },
            { 8 },
        )

        assertEquals(4, project.maxParallelUsagesOf("SomeTask"))
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService sets no limit when the strategy and all fallbacks return null`() {
        val project = buildProject()

        project.registerKotlinNativeParallelismLimitService(
            "SomeTask",
            maxParallelUsagesCalculatorStrategy = { null },
            { null },
        )

        assertNull(project.maxParallelUsagesOf("SomeTask"))
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
