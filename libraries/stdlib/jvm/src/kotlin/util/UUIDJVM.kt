/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import java.nio.*
import java.security.SecureRandom
import kotlin.internal.InlineOnly

private val secureRandom by lazy { SecureRandom() }

internal actual fun secureRandomUUID(): UUID {
    val randomBytes = ByteArray(UUID.SIZE_BYTES)
    secureRandom.nextBytes(randomBytes)
    return uuidFromRandomBytes(randomBytes)
}

/** Converts this [java.util.UUID] value to [kotlin.UUID] value. */
@InlineOnly
public inline fun java.util.UUID.toKotlinUUID(): UUID =
    UUID.fromLongs(mostSignificantBits, leastSignificantBits)

/** Converts this [kotlin.UUID] value to [java.util.UUID] value. */
@InlineOnly
public inline fun UUID.toJavaUUID(): java.util.UUID = toLongs { mostSignificantBits, leastSignificantBits ->
    java.util.UUID(mostSignificantBits, leastSignificantBits)
}

// TODO: ByteBuffer could be configured to have a LITTLE_ENDIAN byte order.
/**
 * Reads the next 16 bytes at this buffer's current position, composing them into a UUID value according
 * to the current byte order, and then increments the position by 16.
 *
 * The returned UUID is equivalent to:
 * ```kotlin
 * val bytes = ByteArray(16)
 * byteBuffer.get(bytes)
 * return UUID.fromByteArray(bytes)
 * ```
 *
 * @throws BufferUnderflowException If there are fewer than 16 bytes remaining in this buffer.
 */
@InlineOnly
public inline fun ByteBuffer.getUUID(): UUID =
    UUID.fromLongs(getLong(), getLong())

// TODO: ByteBuffer could be configured to have a LITTLE_ENDIAN byte order
/**
 * Writes 16 bytes containing the given UUID value, in the current byte order, into this buffer at the current position,
 * and then increments the position by 16.
 *
 * This function is equivalent to:
 * ```kotlin
 * byteBuffer.put(uuid.toByteArray())
 * ```
 *
 * @throws BufferOverflowException If there is insufficient space in this buffer for 16 bytes.
 * @throws ReadOnlyBufferException If this buffer is read-only.
 */
@InlineOnly
public inline fun ByteBuffer.putUUID(uuid: UUID): ByteBuffer = uuid.toLongs { mostSignificantBits, leastSignificantBits ->
    putLong(mostSignificantBits)
    putLong(leastSignificantBits)
}
