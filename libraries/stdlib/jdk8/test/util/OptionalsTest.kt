/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk8.util.test

import java.util.Optional
import kotlin.test.*
import kotlin.util.*

class OptionalsTest {
    @Test
    fun optionalOf() {
        assertEquals(Optional.empty<String>(), optionalOf<String>())
        assertEquals(Optional.empty<String>(), optionalOf<String>(null))
        assertEquals(Optional.of("foo"), optionalOf("foo"))
        assertEquals(Optional.of("foo"), optionalOfNullable("foo"))
    }

    /** Creates an optional for a string not statically known to be non-null. */
    private fun optionalOfNullable(value: String?) = optionalOf(value)

    @Test
    fun getOrNull() {
        assertEquals("foo", optionalOf("foo").getOrNull())
        assertNull(optionalOf<String>().getOrNull())
    }

    @Test
    fun getOrDefault() {
        assertEquals("foo", optionalOf("foo").getOrDefault("bar"))
        assertEquals("bar", optionalOf<String>().getOrDefault("bar"))
    }

    @Test
    fun getOrElse() {
        assertEquals("foo", optionalOf("foo").getOrElse { throw AssertionError() })
        assertEquals("bar", optionalOf<String>().getOrElse { "bar" })

        assertFailsWith<IllegalStateException>(message = "ran") {
            optionalOf<String>().getOrElse { throw IllegalStateException("ran") }
        }
    }

    @Test
    fun optionalToList() {
        assertEquals(listOf("foo"), optionalOf("foo").toList())
        assertEquals(emptyList(), optionalOf<String>().toList())
    }

    @Test
    fun optionalToSet() {
        assertEquals(setOf("foo"), optionalOf("foo").toSet())
        assertEquals(emptySet(), optionalOf<String>().toSet())
    }

    @Test
    fun optionalAsSequence() {
        assertEquals(listOf("foo"), optionalOf("foo").asSequence().toList())
        assertEquals(emptyList(), optionalOf<String>().asSequence().toList())
    }
}