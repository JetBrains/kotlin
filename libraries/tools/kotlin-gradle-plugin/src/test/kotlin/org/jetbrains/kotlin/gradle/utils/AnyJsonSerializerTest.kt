/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AnyJsonSerializerTest {

    @Test
    fun `null produces JsonNull`() {
        assertEquals(JsonNull, AnyJsonSerializer.toJsonElement(null))
    }

    @Test
    fun `primitives are converted`() {
        assertEquals(JsonPrimitive(true), AnyJsonSerializer.toJsonElement(true))
        assertEquals(JsonPrimitive(false), AnyJsonSerializer.toJsonElement(false))
        assertEquals(JsonPrimitive(42), AnyJsonSerializer.toJsonElement(42))
        assertEquals(JsonPrimitive(42L), AnyJsonSerializer.toJsonElement(42L))
        assertEquals(JsonPrimitive(3.14), AnyJsonSerializer.toJsonElement(3.14))
        assertEquals(JsonPrimitive("hello"), AnyJsonSerializer.toJsonElement("hello"))
    }

    @Test
    fun `list of primitives`() {
        val element = AnyJsonSerializer.toJsonElement(listOf(1, "two", true, null))
        assertEquals(
            JsonArray(listOf(JsonPrimitive(1), JsonPrimitive("two"), JsonPrimitive(true), JsonNull)),
            element
        )
    }

    @Test
    fun `map of primitives uses string keys`() {
        val element = AnyJsonSerializer.toJsonElement(mapOf("a" to 1, "b" to "x"))
        assertEquals(
            JsonObject(mapOf("a" to JsonPrimitive(1), "b" to JsonPrimitive("x"))),
            element
        )
    }

    @Test
    fun `nested map and list structure`() {
        val tree = mapOf(
            "main" to listOf("a.js", "b.js"),
            "test" to mapOf(
                "import" to listOf("t1.js"),
                "dependOn" to "main",
            ),
            "flag" to true,
            "nothing" to null,
        )
        val json = AnyJsonSerializer.toJsonString(tree)
        // round-trip parse to assert structure
        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(json)
        assertEquals(AnyJsonSerializer.toJsonElement(tree), parsed)
    }

    @Test
    fun `arrays are supported`() {
        assertEquals(
            JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))),
            AnyJsonSerializer.toJsonElement(intArrayOf(1, 2, 3))
        )
        assertEquals(
            JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b"))),
            AnyJsonSerializer.toJsonElement(arrayOf("a", "b"))
        )
    }

    @Test
    fun `non-string map keys use toString`() {
        val element = AnyJsonSerializer.toJsonElement(mapOf(1 to "one", 2 to "two"))
        assertEquals(
            JsonObject(mapOf("1" to JsonPrimitive("one"), "2" to JsonPrimitive("two"))),
            element
        )
    }

    @Test
    fun `unsupported type at root throws`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            AnyJsonSerializer.toJsonElement(Any())
        }
        assertTrue("root" in ex.message!!, "Message must include path 'root', was: ${ex.message}")
    }

    @Test
    fun `unsupported nested type reports path`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            AnyJsonSerializer.toJsonElement(mapOf("foo" to listOf("ok", Any())))
        }
        assertTrue("root.foo[1]" in ex.message!!, "Message must include path 'root.foo[1]', was: ${ex.message}")
    }

    @Test
    fun `null key in map throws`() {
        val map: Map<Any?, Any?> = mapOf(null to "x")
        assertFailsWith<IllegalArgumentException> {
            AnyJsonSerializer.toJsonElement(map)
        }
    }
}
