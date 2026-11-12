/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnyToJsonElementTest {

    @Serializable
    private data class Options(val modules: Boolean, @SerialName("export_name") val exportName: String)

    private data class PlainOptions(val modules: Boolean, val exportName: String?)

    @Test
    fun `null map values are left out by default`() {
        // webpack's schema rejects an explicit null for many loader and optimization options
        assertEquals(
            buildJsonObject { put("modules", true) },
            anyToJsonElement(mapOf("modules" to true, "exportName" to null)),
        )
    }

    @Test
    fun `null map values are kept with serializeNulls`() {
        assertEquals(
            buildJsonObject {
                put("modules", true)
                put("exportName", JsonNull)
            },
            anyToJsonElement(mapOf("modules" to true, "exportName" to null), serializeNulls = true),
        )
    }

    @Test
    fun `a Serializable class is written by its serializer`() {
        // the SerialName only applies if the generated serializer is used, not the reflective fallback
        assertEquals(
            buildJsonObject {
                put("modules", true)
                put("export_name", "x")
            },
            anyToJsonElement(Options(modules = true, exportName = "x")),
        )
    }

    @Test
    fun `a primitive array is written as a JSON array`() {
        assertEquals(
            buildJsonArray { add(80); add(443) },
            anyToJsonElement(intArrayOf(80, 443)),
        )
    }

    @Test
    fun `any other object is rejected with its type named`() {
        val message = assertFailsWith<IllegalStateException> { anyToJsonElement(PlainOptions(modules = true, exportName = null)) }.message
        assertEquals(
            "Cannot write ${PlainOptions::class.java.name} to a JS configuration file. Pass a Map, a list or a primitive value instead.",
            message,
        )
    }
}
