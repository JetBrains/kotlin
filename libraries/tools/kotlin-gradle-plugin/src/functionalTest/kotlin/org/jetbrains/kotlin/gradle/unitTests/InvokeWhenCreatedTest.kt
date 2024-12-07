/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Named
import org.gradle.api.UnknownDomainObjectException
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail

class InvokeWhenCreatedTest {

    data class Value(private val name: String) : Named {
        override fun getName(): String = name
        override fun toString(): String = name
    }

    private val project = buildProjectWithJvm()
    private val container = project.objects.domainObjectContainer(Value::class.java) { name -> Value(name) }

    @Test
    fun `test - values is already present - is called inline`() {
        var invocations = 0

        container.create("x")
        project.kotlinExtension.apply {
            container.invokeWhenCreated("x") { invocations++; assertEquals("x", name) }
            assertEquals(1, invocations, "Expected 'invokeWhenCreated' to be called inline, once")
        }

        project.evaluate()
        assertEquals(1, invocations, "Expected 'invokeWhenCreated' to be called only once")
    }

    @Test
    fun `test - value will be created later`() {
        var invocations = 0

        project.kotlinExtension.apply {
            container.invokeWhenCreated("x") { invocations++; assertEquals("x", name) }
            assertEquals(0, invocations, "Expected 'invokeWhenCreated' to be not called")
        }

        container.create("y")
        assertEquals(0, invocations, "Expected 'invokeWhenCreated' to be not called")

        container.create("x")
        assertEquals(1, invocations, "Expected 'invokeWhenCreated' to be called once")
    }

    @Test
    fun `test - value is missing - evaluate will fail`() {
        project.kotlinExtension.apply {
            container.invokeWhenCreated("x") {
                fail("Called 'invokeWhenCreated' unexpectedly")
            }
        }

        val causes = assertFails { project.evaluate() }.withLinearClosure { it.cause }
        assertEquals<Class<*>>(UnknownDomainObjectException::class.java, causes.last().javaClass)
    }

    @Test
    fun `test - is lenient up-to last stage of Kotlin Plugin Lifecycle`() = project.runLifecycleAwareTest {
        project.kotlinExtension.apply {
            val invocations = AtomicInteger(0)
            container.invokeWhenCreated("x") { assertEquals(1, invocations.incrementAndGet()) }
            KotlinPluginLifecycle.Stage.last.await()
            assertEquals(0, invocations.get())
            container.create("x")
            assertEquals(1, invocations.get())
        }
    }
}