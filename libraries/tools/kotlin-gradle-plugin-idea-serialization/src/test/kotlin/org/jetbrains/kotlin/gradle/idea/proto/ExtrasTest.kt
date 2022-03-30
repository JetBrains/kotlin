/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.proto.IdeaKotlinExtras
import org.jetbrains.kotlin.gradle.kpm.idea.proto.serialize
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.emptyExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtrasTest {

    object IntSerializer : IdeaKotlinExtrasSerializer<Int> {
        override fun serialize(key: Extras.Key<Int>, extra: Int): ByteArray = byteArrayOf(extra.toByte())
        override fun deserialize(key: Extras.Key<Int>, data: ByteArray): Int = data.single().toInt()
    }

    object StringSerializer : IdeaKotlinExtrasSerializer<String> {
        override fun serialize(key: Extras.Key<String>, extra: String): ByteArray = extra.encodeToByteArray()
        override fun deserialize(key: Extras.Key<String>, data: ByteArray): String = data.decodeToString()
    }

    @Test
    fun `test - empty extras`() {
        val extras = IdeaKotlinExtras(emptyExtras().serialize())
        assertEquals(emptySet(), extras.ids)
    }

    @Test
    fun `test - with values`() {
        val extras = mutableExtrasOf()
        val intKey = extrasKeyOf<Int>()
        val transientKey = extrasKeyOf<String>("transient")
        val retainedKey = extrasKeyOf<String>("retained") + StringSerializer
        val retainedKey2 = extrasKeyOf<Int>("retained") + IntSerializer

        extras[intKey] = 0
        extras[transientKey] = "bye"
        extras[retainedKey] = "hello"
        extras[retainedKey2] = 2411

        val deserialized = IdeaKotlinExtras(extras.serialize())
        assertEquals(setOf(retainedKey.id, retainedKey2.id), deserialized.ids)
        assertNull(deserialized[transientKey])
        assertEquals("hello", extras[retainedKey])
        assertEquals(2411, extras[retainedKey2])
    }
}
