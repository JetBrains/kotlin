/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.restrictWritesToSingleWriterOnly
import kotlin.test.*

class RestrictNamedDomainObjectCollectionWritesTest {
    private data class Foo(val id: Int) : Named {
        override fun getName(): String = "$id"
    }

    private val project = buildProject {}
    private val collection = project.objects.namedDomainObjectSet(Foo::class.java)

    private fun assertThrowsIllegalMutation(code: () -> Unit) {
        val throwable = assertFails { code() }
        assertTrue("Expected illegal mutation error but got: $throwable") {
            throwable.message!!.contains("This collection is read only.")
        }
    }

    @Test
    fun `test - object can't be added after named domain object set is restricted`() {
        collection.restrictWritesToSingleWriterOnly()
        assertThrowsIllegalMutation { collection.add(Foo(1)) }
        assertTrue { collection.isEmpty() }
    }

    @Test
    fun `test - observers is not notified when element is added illegally`() {
        collection.restrictWritesToSingleWriterOnly()
        collection.all { fail("I was unexpectedly notified with $it") }
        assertThrowsIllegalMutation { collection.add(Foo(1)) }
        assertTrue { collection.isEmpty() }
    }

    // This is Implementation Detail behaviour that doesn't affect KGP with current use cases
    // I keep this implementation detail verified by tests to ensure that it isn't changed accidentally
    @Test
    fun `test - observers registered before restriction will be notified on mutation attempt`() {
        val firstCollector = mutableListOf<Foo>()
        collection.all { firstCollector.add(it) }
        val writer = collection.restrictWritesToSingleWriterOnly()
        assertFails { collection.add(Foo(1)) }
        writer.add(Foo(2))
        assertEquals(listOf(Foo(1), Foo(2)), firstCollector)
    }
}