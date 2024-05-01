/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

import java.nio.*
import java.security.SecureRandom
import kotlin.internal.InlineOnly

private val secureRandom by lazy { SecureRandom() }

@ExperimentalStdlibApi
internal actual fun secureRandomUUID(): UUID {
    val randomBytes = ByteArray(UUID.SIZE_BYTES)
    secureRandom.nextBytes(randomBytes)
    return uuidFromRandomBytes(randomBytes)
}

/**
 * Converts this [java.util.UUID] value to the corresponding [kotlin.uuid.UUID] value.
 *
 * This function is convenient when one has a Java UUID and needs to interact with an API that accepts a Kotlin UUID.
 * It can also be used to format or retrieve information that the Java UUID does not provide, e.g., `javaUUID.toKotlinUUID().toByteArray()`.
 *
 * @sample samples.uuid.UUIDs.toKotlinUUID
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@InlineOnly
public inline fun java.util.UUID.toKotlinUUID(): UUID =
    UUID.fromLongs(mostSignificantBits, leastSignificantBits)

/**
 * Converts this [kotlin.uuid.UUID] value to the corresponding [java.util.UUID] value.
 *
 * This function is convenient when one has a Kotlin UUID and needs to interact with an API that accepts a Java UUID.
 * It can also be used to retrieve information that the Kotlin UUID does not provide, e.g., `kotlinUUID.toJavaUUID().variant()`.
 *
 * @sample samples.uuid.UUIDs.toJavaUUID
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@InlineOnly
public inline fun UUID.toJavaUUID(): java.util.UUID = toLongs { mostSignificantBits, leastSignificantBits ->
    java.util.UUID(mostSignificantBits, leastSignificantBits)
}

/**
 * Reads the next 16 bytes at this buffer's current position, composing them into a UUID value according
 * to the current byte order. The function increments this buffer's position by 16.
 *
 * The returned UUID is equivalent to:
 * ```kotlin
 * val bytes = ByteArray(16)
 * byteBuffer.get(bytes)
 * if (byteBuffer.order() == ByteOrder.LITTLE_ENDIAN) {
 *     bytes.reverse()
 * }
 * return UUID.fromByteArray(bytes)
 * ```
 *
 * @throws BufferUnderflowException If there are fewer than 16 bytes remaining in this buffer.
 *
 * @sample samples.uuid.UUIDs.byteBufferPutAndGetUUID
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@InlineOnly
public inline fun ByteBuffer.getUUID(): UUID {
    val msb: Long
    val lsb: Long
    if (order() == ByteOrder.BIG_ENDIAN) {
        msb = getLong()
        lsb = getLong()
    } else {
        lsb = getLong()
        msb = getLong()
    }
    return UUID.fromLongs(msb, lsb)
}

/**
 * Reads the next 16 bytes from this buffer at the specified [index], composing them into a UUID value according
 * to the current byte order. The function doesn't change this buffer's position.
 *
 * The returned UUID is equivalent to:
 * ```kotlin
 * val bytes = ByteArray(16) { i ->
 *     byteBuffer.get(index + i)
 * }
 * if (byteBuffer.order() == ByteOrder.LITTLE_ENDIAN) {
 *     bytes.reverse()
 * }
 * return UUID.fromByteArray(bytes)
 * ```
 *
 * @throws IndexOutOfBoundsException If [index] is negative or `index + 15` is not smaller than this buffer's limit.
 *
 * @sample samples.uuid.UUIDs.byteBufferPutAndGetUUID
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@InlineOnly
public inline fun ByteBuffer.getUUID(index: Int): UUID {
    val msb: Long
    val lsb: Long
    if (order() == ByteOrder.BIG_ENDIAN) {
        msb = getLong(index)
        lsb = getLong(index + 8)
    } else {
        lsb = getLong(index)
        msb = getLong(index + 8)
    }
    return UUID.fromLongs(msb, lsb)
}

/**
 * Writes 16 bytes containing the given UUID value, in the current byte order, into this buffer at the current position.
 * The function increments this buffer's position by 16.
 *
 * This function is equivalent to:
 * ```kotlin
 * val bytes = uuid.toByteArray()
 * if (byteBuffer.order() == ByteOrder.LITTLE_ENDIAN) {
 *     bytes.reverse()
 * }
 * byteBuffer.put(bytes)
 * ```
 *
 * @throws BufferOverflowException If there is insufficient space in this buffer for 16 bytes.
 * @throws ReadOnlyBufferException If this buffer is read-only.
 *
 * @sample samples.uuid.UUIDs.byteBufferPutAndGetUUID
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@InlineOnly
public inline fun ByteBuffer.putUUID(uuid: UUID): ByteBuffer = uuid.toLongs { msb, lsb ->
    if (order() == ByteOrder.BIG_ENDIAN) {
        putLong(msb)
        putLong(lsb)
    } else {
        putLong(lsb)
        putLong(msb)
    }
}

/**
 * Writes 16 bytes containing the given UUID value, in the current byte order, into this buffer at the specified [index].
 * The function doesn't change this buffer's position.
 *
 * This function is equivalent to:
 * ```kotlin
 * val bytes = uuid.toByteArray()
 * if (byteBuffer.order() == ByteOrder.LITTLE_ENDIAN) {
 *     bytes.reverse()
 * }
 * bytes.forEachIndexed { i, byte ->
 *     byteBuffer.put(index + i, byte)
 * }
 * ```
 *
 * @throws IndexOutOfBoundsException If [index] is negative or `index + 15` is not smaller than this buffer's limit.
 * @throws ReadOnlyBufferException If this buffer is read-only.
 *
 * @sample samples.uuid.UUIDs.byteBufferPutAndGetUUID
 */
@SinceKotlin("2.0")
@ExperimentalStdlibApi
@InlineOnly
public inline fun ByteBuffer.putUUID(index: Int, uuid: UUID): ByteBuffer = uuid.toLongs { msb, lsb ->
    if (order() == ByteOrder.BIG_ENDIAN) {
        putLong(index, msb)
        putLong(index + 8, lsb)
    } else {
        putLong(index, lsb)
        putLong(index + 8, msb)
    }
}
