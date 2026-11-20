/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.DefaultTask
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.maxParallelUsagesOf
import org.jetbrains.kotlin.internal.compilerRunner.native.registerKotlinNativeParallelismLimitService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class KotlinNativeToolParallelismLimitServiceTest {

    abstract class FirstTask : DefaultTask()
    abstract class SecondTask : DefaultTask()

    @Test
    fun `registerKotlinNativeParallelismLimitService sets maxParallelUsages`() {
        val project = buildProject()

        project.gradle.registerKotlinNativeParallelismLimitService(FirstTask::class.java, maxParallelUsages = 3)

        assertEquals(3, project.maxParallelUsagesOf<FirstTask>())
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService reuses the same service instance for the same task type`() {
        val project = buildProject()

        val first = project.gradle.registerKotlinNativeParallelismLimitService(FirstTask::class.java, maxParallelUsages = 2)
        val second = project.gradle.registerKotlinNativeParallelismLimitService(FirstTask::class.java, maxParallelUsages = 2)

        assertSame(first.get(), second.get())
    }

    @Test
    fun `registerKotlinNativeParallelismLimitService uses independent services for different task types`() {
        val project = buildProject()

        project.gradle.registerKotlinNativeParallelismLimitService(FirstTask::class.java, maxParallelUsages = 2)
        project.gradle.registerKotlinNativeParallelismLimitService(SecondTask::class.java, maxParallelUsages = 6)

        assertEquals(2, project.maxParallelUsagesOf<FirstTask>())
        assertEquals(6, project.maxParallelUsagesOf<SecondTask>())
    }
}
