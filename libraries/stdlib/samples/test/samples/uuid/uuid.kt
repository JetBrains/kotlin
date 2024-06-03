/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.uuid

import samples.*
import java.nio.*
import kotlin.uuid.*

class UUIDs {

    @Sample
    fun toKotlinUuid() {
        val urlNamespace = UUID.parse("6ba7b811-9dad-11d1-80b4-00c04fd430c8").toByteArray()
        val url = "https://kotlinlang.org/api/core/kotlin-stdlib/".encodeToByteArray()
        val javaUUID = java.util.UUID.nameUUIDFromBytes(urlNamespace + url)

        assertPrints(javaUUID, "49953a09-8fa4-3071-bcd4-d9d3bc84e5b2")
        assertPrints(javaUUID.toKotlinUuid().toHexString(), "49953a098fa43071bcd4d9d3bc84e5b2")
    }

    @Sample
    fun toJavaUuid() {
        val hexString = "550e8400e29b41d4a716446655440000"
        val kotlinUUID = UUID.parseHex(hexString)
        val javaUUID = kotlinUUID.toJavaUuid()

        assertPrints(javaUUID, "550e8400-e29b-41d4-a716-446655440000")
        assertPrints(javaUUID.version(), "4")
    }

    @Sample
    fun byteBufferPut() {
        val uuid = UUID.parse("550e8400-e29b-41d4-a716-446655440000")
        val bytes = ByteArray(16)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.putUuid(uuid)

        // The written 16 bytes are exactly equal to the UUID bytes
        assertPrints(bytes.contentEquals(uuid.toByteArray()), "true")
    }

    @Sample
    fun byteBufferPutAtIndex() {
        val uuid = UUID.parse("550e8400-e29b-41d4-a716-446655440000")
        val bytes = ByteArray(20)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.putUuid(index = 2, uuid)

        // The written 16 bytes are exactly equal to the UUID bytes
        val writtenBytes = bytes.sliceArray(2..<18)
        assertPrints(writtenBytes.contentEquals(uuid.toByteArray()), "true")
    }

    @Sample
    fun byteBufferGet() {
        val uuidBytes = byteArrayOf(
            0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(),
            0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
        )
        val buffer = ByteBuffer.wrap(uuidBytes)
        val uuid = buffer.getUuid()

        // The UUID has exactly the same 16 bytes
        assertPrints(uuid.toByteArray().contentEquals(uuidBytes), "true")
        assertPrints(uuid, "550e8400-e29b-41d4-a716-446655440000")
    }

    @Sample
    fun byteBufferGetByIndex() {
        val bytes = byteArrayOf(
            0x00, 0x00, 0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(),
            0x41, 0xd4.toByte(), 0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
        )
        val buffer = ByteBuffer.wrap(bytes)
        val uuid = buffer.getUuid(index = 2)

        // The UUID has exactly the same 16 bytes
        val uuidBytes = bytes.sliceArray(2..<18)
        assertPrints(uuid.toByteArray().contentEquals(uuidBytes), "true")
        assertPrints(uuid, "550e8400-e29b-41d4-a716-446655440000")
    }

    @Sample
    fun toStringSample() {
        val uuid = UUID.fromULongs(0x550E8400E29B41D4uL, 0xA716446655440000uL)
        assertPrints(uuid.toString(), "550e8400-e29b-41d4-a716-446655440000")
        println(UUID.random().toString())
    }

    @Sample
    fun toHexString() {
        val uuid = UUID.fromULongs(0x550E8400E29B41D4uL, 0xA716446655440000uL)
        assertPrints(uuid.toHexString(), "550e8400e29b41d4a716446655440000")
        println(UUID.random().toHexString())
    }

    @Sample
    fun toByteArray() {
        val uuid = UUID.fromULongs(0x550E8400E29B41D4uL, 0xA716446655440000uL)
        assertPrints(
            uuid.toByteArray().contentToString(),
            "[85, 14, -124, 0, -30, -101, 65, -44, -89, 22, 68, 102, 85, 68, 0, 0]"
        )
        println(UUID.random().toByteArray().contentToString())
    }

    @Sample
    fun uuidEquals() {
        val uuid1 = UUID.fromULongs(0x550E8400E29B41D4uL, 0xA716446655440000uL)
        val uuid2 = UUID.parse("550e8400-e29b-41d4-a716-446655440000")
        val uuid3 = UUID.parse("550e8400-e29b-41d4-a716-446655440001") // the last bit is one

        assertPrints(uuid1 == uuid2, "true")
        assertPrints(uuid1 == uuid3, "false")
    }

    @Sample
    fun fromLongs() {
        val uuid = UUID.fromLongs(6128981282234515924, -6406858213580079104)
        assertPrints(uuid, "550e8400-e29b-41d4-a716-446655440000")
    }

    @Sample
    fun fromULongs() {
        val uuid = UUID.fromULongs(0x550E8400E29B41D4uL, 0xA716446655440000uL)
        assertPrints(uuid, "550e8400-e29b-41d4-a716-446655440000")
    }

    @Sample
    fun fromByteArray() {
        val byteArray = byteArrayOf(
            0x55, 0x0e, 0x84.toByte(), 0x00, 0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(),
            0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0x00, 0x00
        )
        val uuid = UUID.fromByteArray(byteArray)
        assertPrints(uuid, "550e8400-e29b-41d4-a716-446655440000")
    }

    @Sample
    fun parse() {
        val uuid = UUID.parse("550E8400-e29b-41d4-A716-446655440000") // case insensitive
        assertPrints(uuid, "550e8400-e29b-41d4-a716-446655440000")
    }

    @Sample
    fun parseHex() {
        val uuid = UUID.parseHex("550E8400e29b41d4A716446655440000") // case insensitive
        assertPrints(uuid, "550e8400-e29b-41d4-a716-446655440000")
    }

    @Sample
    fun random() {
        // Generates a random and unique UUID each time
        println(UUID.random())
        println(UUID.random())
        println(UUID.random())
    }

    @Sample
    fun lexicalOrder() {
        val uuid1 = UUID.parse("49d6d991-c780-4eb5-8585-5169c25af912")
        val uuid2 = UUID.parse("c0bac692-7208-4448-a8fe-3e3eb128db2a")
        val uuid3 = UUID.parse("49d6d991-aa92-4da0-917e-527c69621cb7")

        val sortedUUIDs = listOf(uuid1, uuid2, uuid3).sortedWith(UUID.LEXICAL_ORDER)

        assertPrints(sortedUUIDs == listOf(uuid3, uuid1, uuid2), "true")
    }
}