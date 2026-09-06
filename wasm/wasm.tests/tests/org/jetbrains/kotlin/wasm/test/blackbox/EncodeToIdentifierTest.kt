/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EncodeToIdentifierTest {
    @Test
    fun `given a typical batch package then letters and digits pass through and the rest is escaped`() {
        assertEquals("__codegen___2e__box___2etestFoo42", "_codegen_._box_.testFoo42".encodeToIdentifier())
    }

    @Test
    fun `given any input then the encoding is a valid identifier fragment`() {
        val inputs = listOf("", "a", "_", "__", "a b", "a/b.kt", "тест", "😀", "\u0000\n|")

        for (input in inputs) {
            val encoded = input.encodeToIdentifier()
            assertTrue(encoded.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' }, "$input -> $encoded")
        }
    }

    @Test
    fun `given inputs that differ only in escapable characters then their encodings stay distinct`() {
        // Every pair here would collide under a naive escape that shares the underscore with the escape prefix.
        val inputs = listOf("a_b", "a__b", "a_2eb", "a.b", "a_5fb", "a__2eb", "a._b", "_", "__", "_5f", "_")

        val encoded = inputs.map { it.encodeToIdentifier() }

        assertEquals(inputs.distinct().size, encoded.distinct().size, inputs.zip(encoded).toString())
    }
}
