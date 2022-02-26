/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk8.jvm.optionals.test

import java.util.Optional
import kotlin.test.*
import kotlin.jvm.optionals.*

class OptionalsTest {
    @Test
    fun getOrNull() {
        assertEquals("foo", Optional.of("foo").getOrNull())
        assertNull(Optional.empty<String>().getOrNull())
    }

    @Test
    fun getOrDefault() {
        assertEquals("foo", Optional.of("foo").getOrDefault("bar"))
        assertEquals("bar", Optional.empty<String>().getOrDefault("bar"))

        // Return type can be a supertype
        assertNull(Optional.empty<String>().getOrDefault(null))
        assertEquals(5.0, Optional.empty<Int>().getOrDefault<Number, Int>(5.0))
    }

    @Test
    fun getOrElse() {
        assertEquals("foo", Optional.of("foo").getOrElse { throw AssertionError() })
        assertEquals("bar", Optional.empty<String>().getOrElse { "bar" })

        assertFailsWith<IllegalStateException>(message = "ran") {
            Optional.empty<String>().getOrElse { throw IllegalStateException("ran") }
        }

        // Return type can be a supertype
        assertNull(Optional.empty<String>().getOrElse { null })
        assertEquals(5.0, Optional.empty<Int>().getOrElse<Number, Int> { 5.0 })
    }

    @Test
    fun optionalToCollection_presentAddsValue() {
        val dest = mutableListOf<CharSequence>()
        Optional.of("foo").toCollection(dest)
        assertEquals(listOf<CharSequence>("foo"), dest)
    }

    @Test
    fun optionalToCollection_emptyAddsNothing() {
        val dest = mutableListOf<String>()
        Optional.empty<String>().toCollection(dest)
        assertEquals(emptyList(), dest)
    }

    @Test
    fun optionalToList() {
        assertEquals(listOf("foo"), Optional.of("foo").toList())
        assertEquals(emptyList(), Optional.empty<String>().toList())
    }

    @Test
    fun optionalToSet() {
        assertEquals(setOf("foo"), Optional.of("foo").toSet())
        assertEquals(emptySet(), Optional.empty<String>().toSet())
    }

    @Test
    fun optionalAsSequence() {
        assertEquals(listOf("foo"), Optional.of("foo").asSequence().toList())
        assertEquals(emptyList(), Optional.empty<String>().asSequence().toList())
    }
}
