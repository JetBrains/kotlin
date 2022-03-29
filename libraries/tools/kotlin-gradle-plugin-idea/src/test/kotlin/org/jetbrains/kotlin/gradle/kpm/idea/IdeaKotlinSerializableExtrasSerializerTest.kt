/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinExtrasSerializer.ErrorHandler.StreamLogger
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.Serializable
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdeaKotlinSerializableExtrasSerializerTest {

    data class NotDeserializable(val value: Int = 0) : Serializable {
        fun readResolve(): Any = error("This class cannot be deserialized!")
    }

    @Test
    fun `test - serialize and deserialize string`() {
        val serializer = IdeaKotlinExtrasSerializer.serializable<String>()
        val key = extrasKeyOf<String>() + serializer
        assertEquals("Hello there", serializer.deserialize(key, serializer.serialize(key, "Hello there")))
    }

    @Test
    fun `test - default error handling`() {
        val byteStream = ByteArrayOutputStream()
        val logStream = PrintStream(byteStream)

        val serializer = IdeaKotlinExtrasSerializer.serializable<NotDeserializable>(StreamLogger(logStream))
        val key = extrasKeyOf<NotDeserializable>() + serializer

        val data = serializer.serialize(key, NotDeserializable())

        logStream.flush()
        assertTrue(byteStream.toByteArray().isEmpty())

        assertNull(serializer.deserialize(key, data))
        logStream.flush()
        assertTrue(byteStream.toByteArray().decodeToString().contains("Failed to deserialize"))
        assertTrue(byteStream.toByteArray().decodeToString().contains("This class cannot be deserialized!"))
    }

    @Test
    fun `test - special error handling`() {
        val byteStream = ByteArrayOutputStream()
        val logStream = PrintStream(byteStream)

        val serializer = IdeaKotlinExtrasSerializer.serializable<NotDeserializable>(StreamLogger(logStream))
        val key = extrasKeyOf<NotDeserializable>() + serializer + object : IdeaKotlinExtrasSerializer.ErrorHandler<NotDeserializable> {
            override fun onDeserializationFailure(key: Extras.Key<NotDeserializable>, error: Throwable): NotDeserializable {
                return NotDeserializable(2411) // <- recovery!
            }
        }

        val data = serializer.serialize(key, NotDeserializable())
        assertEquals(NotDeserializable(2411), serializer.deserialize(key, data))
    }
}
