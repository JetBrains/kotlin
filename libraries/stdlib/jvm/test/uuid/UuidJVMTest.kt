/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.uuid

import test.io.serializeAndDeserialize
import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*
import kotlin.uuid.*

typealias JavaUUID = java.util.UUID

class UuidJVMTest {
    private val uuidByteArray = byteArrayOf(
        0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(),
        0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
    )
    private val uuidString = "550e8400-e29b-41d4-a716-446655440000"
    private val uuid = Uuid.parse(uuidString)

    private val intValue: Int = 0xC9F350C4.toInt()
    private val byteValue: Byte = 0x9E.toByte()

    @Test
    fun toJavaUuid() {
        Uuid.NIL.let {
            assertEquals(it.toString(), it.toJavaUuid().toString())
        }
        Uuid.fromLongs(-1, -1).let {
            assertEquals(it.toString(), it.toJavaUuid().toString())
        }
        uuid.let {
            assertEquals(uuidString, it.toJavaUuid().toString())
        }
        Uuid.random().let {
            assertEquals(it.toString(), it.toJavaUuid().toString())
        }
    }

    @Test
    fun toKotlinUuid() {
        JavaUUID(0, 0).let {
            assertEquals(it.toString(), it.toKotlinUuid().toString())
        }
        JavaUUID(-1, -1).let {
            assertEquals(it.toString(), it.toKotlinUuid().toString())
        }
        JavaUUID.fromString(uuidString).let {
            assertEquals(uuidString, it.toKotlinUuid().toString())
        }
        JavaUUID.randomUUID().let {
            assertEquals(it.toString(), it.toKotlinUuid().toString())
        }
    }

    @Test
    fun getUuid() {
        for (byteOrder in listOf(ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN)) {
            ByteBuffer.allocate(32).apply {
                order(byteOrder) // buffer's byte order does not affect uuid

                put(byteValue) // index = 0
                put(uuidByteArray) // index = 1
                putInt(intValue) // index = 17

                position(0)

                assertEquals(byteValue, get())
                assertEquals(uuid, getUuid())
                assertEquals(intValue, getInt())

                assertEquals(21, position())
                assertEquals(uuid, getUuid(1))
                assertEquals(21, position()) // Hasn't changed
            }
        }
        ByteBuffer.allocate(16).apply {
            put(uuidByteArray)
            position(1)

            assertFailsWith<BufferUnderflowException> {
                getUuid()
            }
            assertFailsWith<IndexOutOfBoundsException> {
                getUuid(1)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                getUuid(index = -1) // negative index
            }
        }
    }

    @Test
    fun putUuid() {
        val uuid2 = Uuid.parse("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
        for (byteOrder in listOf(ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN)) {
            ByteBuffer.allocate(32).apply {
                order(byteOrder) // buffer's byte order does not affect uuid

                put(byteValue) // index = 0
                putUuid(uuid) // index = 1
                putInt(intValue) // index = 17

                position(0)

                assertEquals(byteValue, get())
                assertContentEquals(uuidByteArray, ByteArray(16).also { get(it) })
                assertEquals(intValue, getInt())

                assertEquals(21, position())
                putUuid(1, uuid2)
                assertEquals(21, position()) // Hasn't changed
                assertContentEquals(uuid2.toByteArray(), ByteArray(16) { i -> get(1 + i) })
            }
        }
        ByteBuffer.allocate(16).apply {
            put(byteValue)
            assertFailsWith<BufferOverflowException> {
                putUuid(uuid)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                putUuid(1, uuid)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                putUuid(-1, uuid) // negative index
            }
        }
    }

    @Test
    fun testHashCode() {
        assertEquals(uuid.hashCode(), uuid.toJavaUuid().hashCode())
        assertEquals(Uuid.NIL.hashCode(), JavaUUID(0, 0).hashCode())
        assertEquals(Uuid.fromLongs(-1, -1).hashCode(), JavaUUID(-1, -1).hashCode())
    }

    @Test
    fun serialize() {
        fun testSerializable(uuid: Uuid) {
            val result = serializeAndDeserialize(uuid)
            assertEquals(uuid, result)
        }

        testSerializable(uuid)
        testSerializable(Uuid.NIL)
        testSerializable(Uuid.fromLongs(-1, -1))
    }
}