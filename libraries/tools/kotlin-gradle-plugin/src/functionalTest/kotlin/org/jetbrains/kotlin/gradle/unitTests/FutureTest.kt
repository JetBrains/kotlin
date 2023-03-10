/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.FinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.currentKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.startKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.future
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FutureTest {

    private val project = buildProject().also { project ->
        project.startKotlinPluginLifecycle()
    }

    @Test
    fun `test - simple deferred future`() = project.runLifecycleAwareTest {
        val future = project.future {
            await(FinaliseDsl)
            42
        }

        assertFailsWith<IllegalLifecycleException> { future.getOrThrow() }
        assertEquals(42, future.await())
        assertEquals(42, future.getOrThrow())
        assertEquals(FinaliseDsl, currentKotlinPluginLifecycle().stage)
    }

    @Test
    fun `test - future depending on another future`() = project.runLifecycleAwareTest {
        val futureA = project.future {
            await(FinaliseDsl)
            42
        }

        val futureB = project.future {
            futureA.await().toString()
        }

        assertEquals("42", futureB.await())
        assertEquals("42", futureB.getOrThrow())
        assertEquals(FinaliseDsl, currentKotlinPluginLifecycle().stage)
    }

    @Test
    fun `test - after lifecycle finished`() {
        val project = buildProject()
        project.startKotlinPluginLifecycle()
        project.evaluate()

        val future = project.future {
            await(FinaliseDsl)
            42
        }

        assertEquals(42, future.getOrThrow())
    }
}