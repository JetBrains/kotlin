/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("UuidKt")

package kotlin.uuid

import java.nio.*
import java.security.SecureRandom

private object SecureRandomHolder {
    val instance = SecureRandom()
}

@ExperimentalStdlibApi
internal actual fun secureRandomUuid(): Uuid {
    val randomBytes = ByteArray(Uuid.SIZE_BYTES)
    SecureRandomHolder.instance.nextBytes(randomBytes)
    return uuidFromRandomBytes(randomBytes)
}

/**
 * Converts this [java.util.UUID] value to the corresponding [kotlin.uuid.Uuid] value.
 *
 * This function is convenient when one has a Java uuid and needs to interact with an API
 * that accepts a Kotlin uuid. It can also be used to format or retrieve information that
 * the Java uuid does not provide, e.g., `javaUuid.toKotlinUuid().toByteArray()`.
 *
 * @sample samples.uuid.Uuids.toKotlinUuid
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@Suppress("NOTHING_TO_INLINE")
public inline fun java.util.UUID.toKotlinUuid(): Uuid =
    Uuid.fromLongs(mostSignificantBits, leastSignificantBits)

/**
 * Converts this [kotlin.uuid.Uuid] value to the corresponding [java.util.UUID] value.
 *
 * This function is convenient when one has a Kotlin uuid and needs to interact with an
 * API that accepts a Java uuid.
 *
 * @sample samples.uuid.Uuids.toJavaUuid
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@Suppress("NOTHING_TO_INLINE")
public inline fun Uuid.toJavaUuid(): java.util.UUID = toLongs { mostSignificantBits, leastSignificantBits ->
    java.util.UUID(mostSignificantBits, leastSignificantBits)
}

/**
 * Reads a [Uuid] value at this buffer's current position.
 *
 * This function reads the next 16 bytes at this buffer's current [position][Buffer.position]
 * and assembles a uuid from them. As a result, the buffer's position is incremented by 16.
 *
 * Note that this function ignores the buffer's [byte order][ByteBuffer.order].
 * The 16 bytes are read sequentially, with each byte representing the next 8 bits of the uuid,
 * starting from the first byte representing the most significant 8 bits to the last byte
 * representing the least significant 8 bits.
 *
 * The returned uuid is equivalent to:
 * ```kotlin
 * val bytes = ByteArray(16)
 * byteBuffer.get(bytes)
 * return Uuid.fromByteArray(bytes)
 * ```
 *
 * @return The uuid value read at this buffer's current position.
 * @throws BufferUnderflowException If there are fewer than 16 bytes remaining in this buffer.
 *
 * @see Uuid.toByteArray
 * @sample samples.uuid.Uuids.byteBufferGet
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
public fun ByteBuffer.getUuid(): Uuid {
    if (position() + 15 >= limit()) {
        throw BufferUnderflowException() // otherwise a partial read could occur
    }
    var msb = getLong()
    var lsb = getLong()
    if (order() == ByteOrder.LITTLE_ENDIAN) {
        msb = msb.reverseBytes()
        lsb = lsb.reverseBytes()
    }
    return Uuid.fromLongs(msb, lsb)
}

/**
 * Reads a [Uuid] value at the specified [index].
 *
 * This function reads the next 16 bytes from this buffer at the specified [index]
 * and assembles a uuid from them. The buffer's [position][ByteBuffer.position], however, is not updated.
 *
 * Note that this function ignores the buffer's [byte order][ByteBuffer.order].
 * The 16 bytes are read sequentially, with each byte representing the next 8 bits of the uuid,
 * starting from the first byte representing the most significant 8 bits to the last byte
 * representing the least significant 8 bits.
 *
 * The returned uuid is equivalent to:
 * ```kotlin
 * val bytes = ByteArray(16) { i ->
 *     byteBuffer.get(index + i)
 * }
 * return Uuid.fromByteArray(bytes)
 * ```
 * except that this function first checks that there are sufficient bytes in the buffer.
 *
 * @param index The index to read a uuid at.
 * @return The uuid value read at the specified [index].
 * @throws IndexOutOfBoundsException If [index] is negative or `index + 15` is not smaller than
 *   this buffer's [limit][Buffer.limit].
 *
 * @see Uuid.fromByteArray
 * @sample samples.uuid.Uuids.byteBufferGetByIndex
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
public fun ByteBuffer.getUuid(index: Int): Uuid {
    if (index < 0) {
        throw IndexOutOfBoundsException("Negative index: $index")
    } else if (index + 15 >= limit()) {
        throw IndexOutOfBoundsException("Not enough bytes to read a uuid at index: $index, with limit: ${limit()} ")
    }
    var msb = getLong(index)
    var lsb = getLong(index + 8)
    if (order() == ByteOrder.LITTLE_ENDIAN) {
        msb = msb.reverseBytes()
        lsb = lsb.reverseBytes()
    }
    return Uuid.fromLongs(msb, lsb)
}

/**
 * Writes the specified [uuid] value at this buffer's current position.
 *
 * This function writes 16 bytes containing the given uuid value into this buffer at the current
 * [position][Buffer.position]. As a result, the buffer's position is incremented by 16.
 *
 * Note that this function ignores the buffer's [byte order][ByteBuffer.order].
 * The 16 bytes are written sequentially, with each byte representing the next 8 bits of the uuid,
 * starting from the first byte representing the most significant 8 bits to the last byte
 * representing the least significant 8 bits.
 *
 * This function is equivalent to:
 * ```kotlin
 * byteBuffer.put(uuid.toByteArray())
 * ```
 *
 * @param uuid The uuid value to write.
 * @return This byte buffer.
 * @throws BufferOverflowException If there is insufficient space in this buffer for 16 bytes.
 * @throws ReadOnlyBufferException If this buffer is read-only.
 *
 * @see Uuid.toByteArray
 * @sample samples.uuid.Uuids.byteBufferPut
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
public fun ByteBuffer.putUuid(uuid: Uuid): ByteBuffer = uuid.toLongs { msb, lsb ->
    if (position() + 15 >= limit()) {
        throw BufferOverflowException() // otherwise a partial write could occur
    }
    if (order() == ByteOrder.BIG_ENDIAN) {
        putLong(msb)
        putLong(lsb)
    } else {
        putLong(msb.reverseBytes())
        putLong(lsb.reverseBytes())
    }
}

/**
 * Writes the specified [uuid] value at the specified [index].
 *
 * This function writes 16 bytes containing the given uuid value into this buffer at the
 * specified [index]. The buffer's [position][ByteBuffer.position], however, is not updated.
 *
 * Note that this function ignores the buffer's [byte order][ByteBuffer.order].
 * The 16 bytes are written sequentially, with each byte representing the next 8 bits of the uuid,
 * starting from the first byte representing the most significant 8 bits to the last byte
 * representing the least significant 8 bits.
 *
 * This function is equivalent to:
 * ```kotlin
 * val bytes = uuid.toByteArray()
 * bytes.forEachIndexed { i, byte ->
 *     byteBuffer.put(index + i, byte)
 * }
 * ```
 * except that this function first checks that there is sufficient space in the buffer.
 *
 * @param index The index to write the specified uuid value at.
 * @param uuid The uuid value to write.
 * @return This byte buffer.
 * @throws IndexOutOfBoundsException If [index] is negative or `index + 15` is not smaller than
 *   this buffer's [limit][Buffer.limit].
 * @throws ReadOnlyBufferException If this buffer is read-only.
 *
 * @see Uuid.toByteArray
 * @sample samples.uuid.Uuids.byteBufferPutAtIndex
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
public fun ByteBuffer.putUuid(index: Int, uuid: Uuid): ByteBuffer = uuid.toLongs { msb, lsb ->
    if (index < 0) {
        throw IndexOutOfBoundsException("Negative index: $index")
    } else if (index + 15 >= limit()) {
        throw IndexOutOfBoundsException("Not enough capacity to write a uuid at index: $index, with limit: ${limit()} ")
    }
    if (order() == ByteOrder.BIG_ENDIAN) {
        putLong(index, msb)
        putLong(index + 8, lsb)
    } else {
        putLong(index, msb.reverseBytes())
        putLong(index + 8, lsb.reverseBytes())
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.reverseBytes(): Long = java.lang.Long.reverseBytes(this)