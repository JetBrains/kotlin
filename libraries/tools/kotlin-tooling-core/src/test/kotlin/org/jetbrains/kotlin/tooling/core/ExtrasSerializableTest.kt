/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ExtrasSerializableTest {

    @Test
    fun `test - serialize deserialize - MutableExtras`() {
        val extras = mutableExtrasOf(extrasKeyOf<String>() withValue "hello")
        val deserializedExtras = extras.serialize().deserialize<Extras>()
        assertNotSame(extras, deserializedExtras)
        assertEquals(extras, deserializedExtras)
    }

    @Test
    fun `test - serialize deserialize - EmptyExtras`() {
        assertSame(emptyExtras(), emptyExtras())
        assertSame(emptyExtras(), emptyExtras().serialize().deserialize())
    }

    @Test
    fun `test - serialize deserialize - ImmutableExtras`() {
        val extras = extrasOf(extrasKeyOf<String>() withValue "hello")
        val deserializedExtras = extras.serialize().deserialize<Extras>()
        assertNotSame(extras, deserializedExtras)
        assertEquals(extras, deserializedExtras)
        assertSame(extras.javaClass, deserializedExtras.javaClass)
    }

    private fun Any.serialize(): ByteArray {
        return ByteArrayOutputStream().use { byteArrayOutputStream ->
            ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream -> objectOutputStream.writeObject(this) }
            byteArrayOutputStream.toByteArray()
        }
    }

    private inline fun <reified T : Any> ByteArray.deserialize(): T {
        return ObjectInputStream(ByteArrayInputStream(this)).use { stream ->
            stream.readObject() as T
        }
    }
}

