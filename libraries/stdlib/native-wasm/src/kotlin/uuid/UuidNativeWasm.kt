/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.uuid

@ExperimentalUuidApi
internal actual fun serializedUuid(uuid: Uuid): Any =
    throw UnsupportedOperationException("Serialization is supported only in Kotlin/JVM")

@ExperimentalUuidApi
internal actual fun ByteArray.getLongAt(index: Int): Long =
    getLongAtCommonImpl(index)

@ExperimentalUuidApi
internal actual fun Long.formatBytesInto(dst: ByteArray, dstOffset: Int, startIndex: Int, endIndex: Int) =
    formatBytesIntoCommonImpl(dst, dstOffset, startIndex, endIndex)

@ExperimentalUuidApi
internal actual fun ByteArray.setLongAt(index: Int, value: Long) =
    setLongAtCommonImpl(index, value)

@ExperimentalUuidApi
internal actual fun uuidParseHexDash(hexDashString: String): Uuid =
    uuidParseHexDashCommonImpl(hexDashString)

@ExperimentalUuidApi
internal actual fun uuidParseHex(hexString: String): Uuid =
    uuidParseHexCommonImpl(hexString)