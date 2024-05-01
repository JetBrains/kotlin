/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.uuid

import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*
import kotlin.uuid.*

typealias JavaUUID = java.util.UUID

class UUIDJVMTest {
    private val uuidByteArray = byteArrayOf(
        0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(),
        0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
    )
    private val uuidString = "550e8400-e29b-41d4-a716-446655440000"
    private val uuid = UUID.parse(uuidString)

    private val intValue: Int = 0xC9F350C4.toInt()
    private val byteValue: Byte = 0x9E.toByte()

    @Test
    fun toJavaUUID() {
        UUID.NIL.let {
            assertEquals(it.toString(), it.toJavaUUID().toString())
        }
        UUID.fromLongs(-1, -1).let {
            assertEquals(it.toString(), it.toJavaUUID().toString())
        }
        uuid.let {
            assertEquals(uuidString, it.toJavaUUID().toString())
        }
        UUID.random().let {
            assertEquals(it.toString(), it.toJavaUUID().toString())
        }
    }

    @Test
    fun toKotlinUUID() {
        JavaUUID(0, 0).let {
            assertEquals(it.toString(), it.toKotlinUUID().toString())
        }
        JavaUUID(-1, -1).let {
            assertEquals(it.toString(), it.toKotlinUUID().toString())
        }
        JavaUUID.fromString(uuidString).let {
            assertEquals(uuidString, it.toKotlinUUID().toString())
        }
        JavaUUID.randomUUID().let {
            assertEquals(it.toString(), it.toKotlinUUID().toString())
        }
    }

    @Test
    fun getUUID() {
        ByteBuffer.allocate(32).apply {
            put(byteValue) // index = 0
            put(uuidByteArray) // index = 1
            putInt(intValue) // index = 17

            position(0)

            assertEquals(byteValue, get())
            assertEquals(uuid, getUUID())
            assertEquals(intValue, getInt())

            assertEquals(21, position())
            assertEquals(uuid, getUUID(1))
            assertEquals(21, position()) // Hasn't changed
        }
        ByteBuffer.allocate(32).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            put(byteValue) // index = 0
            put(uuidByteArray.reversedArray()) // index = 1
            putInt(intValue) // index = 17

            position(0)

            assertEquals(byteValue, get())
            assertEquals(uuid, getUUID())
            assertEquals(intValue, getInt())

            assertEquals(21, position())
            assertEquals(uuid, getUUID(1))
            assertEquals(21, position()) // Hasn't changed
        }
        ByteBuffer.allocate(16).apply {
            put(uuidByteArray)
            position(0)

            get() // index = 1
            assertFailsWith<BufferUnderflowException> {
                getUUID()
            }
            assertFailsWith<IndexOutOfBoundsException> {
                getUUID(1)
            }
        }
    }

    @Test
    fun putUUID() {
        val uuid2 = UUID.parse("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
        ByteBuffer.allocate(32).apply {
            put(byteValue) // index = 0
            putUUID(uuid) // index = 1
            putInt(intValue) // index = 17

            position(0)

            assertEquals(byteValue, get())
            assertContentEquals(uuidByteArray, ByteArray(16).also { get(it) })
            assertEquals(intValue, getInt())

            assertEquals(21, position())
            putUUID(1, uuid2)
            assertEquals(21, position()) // Hasn't changed
            assertEquals(uuid2, getUUID(1))
        }
        ByteBuffer.allocate(32).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            put(byteValue) // index = 0
            putUUID(uuid) // index = 1
            putInt(intValue) // index = 17

            position(0)

            assertEquals(byteValue, get())
            assertContentEquals(uuidByteArray.reversedArray(), ByteArray(16).also { get(it) })
            assertEquals(intValue, getInt())

            assertEquals(21, position())
            putUUID(1, uuid2)
            assertEquals(21, position()) // Hasn't changed
            assertEquals(uuid2, getUUID(1))
        }
        ByteBuffer.allocate(16).apply {
            put(byteValue)
            assertFailsWith<BufferOverflowException> {
                putUUID(uuid)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                putUUID(1, uuid)
            }
        }
    }
}