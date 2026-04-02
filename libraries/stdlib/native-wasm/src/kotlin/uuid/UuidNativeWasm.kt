/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

internal actual fun serializedUuid(uuid: Uuid): Any =
    throw UnsupportedOperationException("Serialization is supported only in Kotlin/JVM")

internal actual fun ByteArray.getLongAt(index: Int): Long =
    getLongAtCommonImpl(index)

internal actual fun Long.formatBytesInto(dst: ByteArray, dstOffset: Int, startIndex: Int, endIndex: Int) =
    formatBytesIntoCommonImpl(dst, dstOffset, startIndex, endIndex)

internal actual fun ByteArray.setLongAt(index: Int, value: Long) =
    setLongAtCommonImpl(index, value)

internal actual fun uuidParseHexDash(hexDashString: String): Uuid =
    uuidParseHexDashCommonImpl(hexDashString)

@ExperimentalUuidApi
internal actual fun uuidParseHexDashOrNull(hexDashString: String): Uuid? =
    uuidParseHexDashOrNullCommonImpl(hexDashString)

internal actual fun uuidParseHex(hexString: String): Uuid =
    uuidParseHexCommonImpl(hexString)

@ExperimentalUuidApi
internal actual fun uuidParseHexOrNull(hexString: String): Uuid? =
    uuidParseHexOrNullCommonImpl(hexString)
