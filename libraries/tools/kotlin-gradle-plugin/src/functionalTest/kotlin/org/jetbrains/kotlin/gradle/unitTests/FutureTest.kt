/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.deserialize
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.serialize
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.CoroutineStart
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.FinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.ReadyForExecution
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.*
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class FutureTest {

    private val project = buildProject().also { project ->
        project.startKotlinPluginLifecycle()
    }

    @Test
    fun `test - simple deferred future`() = project.runLifecycleAwareTest {
        val future = project.future {
            FinaliseDsl.await()
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
            FinaliseDsl.await()
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
            FinaliseDsl.await()
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

    @Test
    fun `test - lazy future`() = project.runLifecycleAwareTest {
        val futureInvocations = AtomicInteger(0)
        val future = project.lazyFuture {
            assertEquals(1, futureInvocations.incrementAndGet())
        }

        project.launchInStage(KotlinPluginLifecycle.Stage.last) {
            assertEquals(0, futureInvocations.get())
            future.await()
            assertEquals(1, futureInvocations.get())
        }
    }

    @Test
    fun `test - future map`() = project.runLifecycleAwareTest {
        val future = project.future { "1" }
        val transformInvocations = AtomicInteger(0)

        val mappedFuture = future.map { value ->
            transformInvocations.incrementAndGet()
            value.toInt()
        }

        assertEquals(1, mappedFuture.await())
        assertEquals(1, mappedFuture.getOrThrow())
        assertEquals(1, transformInvocations.get())

        val deserializedMappedFuture = mappedFuture.serialize().deserialize() as Future<*>
        assertEquals(1, deserializedMappedFuture.await())
        assertEquals(1, deserializedMappedFuture.getOrThrow())
    }

    @Test
    fun `test - future map - getOrThrow - exception`() = project.runLifecycleAwareTest {
        val future = project.future {
            ReadyForExecution.await()
            "1"
        }

        val mappedFuture = future.map {
            it.toInt()
        }

        assertFailsWith<IllegalLifecycleException> { mappedFuture.getOrThrow() }
        future.await()
        assertEquals(1, mappedFuture.getOrThrow())
    }

    @Test
    fun `test - future is launched 'Undispatched' by default`() = project.runLifecycleAwareTest {
        val future = project.future { }
        future.getOrThrow()
    }

    @Test
    fun `test - future with CoroutineStart 'Default'`() = project.runLifecycleAwareTest {
        val future = project.future(CoroutineStart.Default) {}
        assertFailsWith<IllegalLifecycleException> { future.getOrThrow() }
        future.await()
    }

    @Test
    fun `test - future isCompleted`() = project.runLifecycleAwareTest {
        val future = CompletableFuture<Unit>()
        assertFalse(future.isCompleted)

        launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseDsl) {
            assertFalse(future.isCompleted)
            future.complete(Unit)
            assertTrue(future.isCompleted)
        }

        launch {
            future.await()
            assertTrue(future.isCompleted)
        }
    }
}
