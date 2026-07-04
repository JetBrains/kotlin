/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package test.collections

import kotlin.collections.builders.MapBuilder
import kotlin.collections.builders.SetBuilder
import test.io.deserializeFromByteArray
import test.io.serializeToByteArray
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InvalidObjectException
import java.io.ObjectStreamClass
import java.io.ObjectStreamConstants
import kotlin.test.*

/**
 * Error paths of the built-map serialization proxy and direct-deserialization rejection of the
 * builder classes (KT-83662): built maps serialize through the SerializedMap proxy, whose
 * stream format carries a flags byte and a size; corrupt values must be rejected on
 * deserialization, and a stream carrying a MapBuilder or SetBuilder instance directly must be
 * rejected too. The happy-path round-trips are covered by
 * CollectionJVMTest.builtMapIsSerializable/builtSetIsSerializable; the buildSet proxy
 * (SerializedCollection) lives in ListBuilder.kt, outside the hashtable scope.
 */
class MapBuilderSerializationTest {

    /** An unknown flags value in the serialized-map payload is rejected. */
    @Test
    fun deserializationRejectsUnknownFlags() {
        val bytes = serialize(buildMap { put(1, "a"); put(2, "b") })
        val patched = patchProxyPayload(bytes, entryCount = 2) { payload -> payload[0] = 1 }
        val e = assertFailsWith<InvalidObjectException> { deserialize(patched) }
        assertTrue(e.message!!.contains("flags"), e.message)
    }

    /** A negative size in the serialized-map payload is rejected before any entry is read. */
    @Test
    fun deserializationRejectsNegativeSize() {
        val bytes = serialize(buildMap { put(1, "a"); put(2, "b") })
        val patched = patchProxyPayload(bytes, entryCount = 2) { payload ->
            payload[1] = -1; payload[2] = -1; payload[3] = -1; payload[4] = -1
        }
        val e = assertFailsWith<InvalidObjectException> { deserialize(patched) }
        assertTrue(e.message!!.contains("size"), e.message)
    }

    /**
     * A deserialized empty built map resolves back to the shared empty-map singleton. The
     * identity assertion is this test's point: the covered proxy code is also exercised by the
     * non-empty round-trips, but nothing else pins that readResolve restores THE singleton
     * rather than an equal copy.
     */
    @Test
    fun deserializedEmptyMapIsSingleton() {
        val empty = buildMap<Any?, Any?> {}
        val result = deserialize(serialize(empty))
        assertSame<Any?>(empty, result)
    }

    /**
     * The builder classes deserialize only through their serialization proxies: a handcrafted
     * stream containing a MapBuilder or SetBuilder instance directly (which regular
     * serialization can never produce because of writeReplace) is rejected by readObject.
     */
    @Test
    fun directDeserializationIsRejected() {
        for (clazz in listOf(MapBuilder::class.java, SetBuilder::class.java)) {
            val e = assertFailsWith<InvalidObjectException>(clazz.simpleName) {
                deserialize(streamWithInstanceOf(clazz))
            }
            assertTrue(e.message!!.contains("proxy"), "the builders' readObject must reject the stream: ${e.message}")
        }
    }

    private fun serialize(value: Any?): ByteArray = serializeToByteArray(value)

    private fun deserialize(bytes: ByteArray): Any? = deserializeFromByteArray(bytes)

    /**
     * Locates the externalizable payload of the SerializedMap proxy inside real serialized
     * bytes and lets the caller corrupt it. The payload is a block-data record: marker 0x77,
     * length 5, then the flags byte and the big-endian entry count.
     */
    private fun patchProxyPayload(bytes: ByteArray, entryCount: Int, patch: (ByteArray) -> Unit): ByteArray {
        val expected = byteArrayOf(
            0x77, 5, 0,
            (entryCount ushr 24).toByte(), (entryCount ushr 16).toByte(),
            (entryCount ushr 8).toByte(), entryCount.toByte()
        )
        val positions = (0..bytes.size - expected.size).filter { start ->
            expected.indices.all { bytes[start + it] == expected[it] }
        }
        assertEquals(1, positions.size, "expected exactly one proxy payload in the stream")
        val result = bytes.copyOf()
        val payload = ByteArray(5) { result[positions.single() + 2 + it] }
        patch(payload)
        payload.forEachIndexed { i, b -> result[positions.single() + 2 + i] = b }
        return result
    }

    /** A minimal valid stream declaring a single instance of [clazz] with no fields. */
    private fun streamWithInstanceOf(clazz: Class<*>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeShort(ObjectStreamConstants.STREAM_MAGIC.toInt())
            out.writeShort(ObjectStreamConstants.STREAM_VERSION.toInt())
            out.writeByte(ObjectStreamConstants.TC_OBJECT.toInt())
            out.writeByte(ObjectStreamConstants.TC_CLASSDESC.toInt())
            out.writeUTF(clazz.name)
            out.writeLong(ObjectStreamClass.lookup(clazz).serialVersionUID)
            out.writeByte(ObjectStreamConstants.SC_SERIALIZABLE.toInt())
            out.writeShort(0) // no serializable fields
            out.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA.toInt())
            out.writeByte(ObjectStreamConstants.TC_NULL.toInt()) // no superclass descriptor
        }
        return bytes.toByteArray()
    }
}
