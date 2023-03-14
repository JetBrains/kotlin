/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.deserialize
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.serialize
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.FinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.currentKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.startKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.LenientFuture
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.lenient
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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

    @Test
    fun `test - lenient future`() {
        val future = CompletableFuture<Int>()
        assertNull(future.lenient.getOrNull())
        assertThrows<IllegalLifecycleException> { future.lenient.getOrThrow() }

        future.complete(42)
        assertEquals(42, future.lenient.getOrThrow())
        assertEquals(42, future.lenient.getOrNull())
    }

    @Test
    fun `test - lenient future serialize`() {
        val future = CompletableFuture<Int>()
        assertFailsWith<IllegalLifecycleException> { future.serialize() }

        run {
            val futureBinary = future.lenient.serialize()
            val deserializedFuture = futureBinary.deserialize() as LenientFuture<*>
            assertNull(deserializedFuture.getOrNull())
        }

        run {
            future.complete(42)
            val futureBinary = future.lenient.serialize()
            val deserializedFuture = futureBinary.deserialize() as LenientFuture<*>
            assertEquals(42, deserializedFuture.getOrNull())
        }
    }
}
