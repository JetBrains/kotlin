/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.encoding

@SinceKotlin("1.8")
@ExperimentalEncodingApi
@kotlin.internal.InlineOnly
internal actual inline fun Base64.platformCharsToBytes(source: CharSequence, startIndex: Int, endIndex: Int): ByteArray {
    return charsToBytesImpl(source, startIndex, endIndex)
}


@SinceKotlin("1.8")
@ExperimentalEncodingApi
@kotlin.internal.InlineOnly
internal actual inline fun Base64.platformEncodeToString(source: ByteArray, startIndex: Int, endIndex: Int): String {
    val byteResult = encodeToByteArrayImpl(source, startIndex, endIndex)
    return bytesToStringImpl(byteResult)
}

@SinceKotlin("1.8")
@ExperimentalEncodingApi
@kotlin.internal.InlineOnly
internal actual inline fun Base64.platformEncodeIntoByteArray(
    source: ByteArray,
    destination: ByteArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
): Int {
    return encodeIntoByteArrayImpl(source, destination, destinationOffset, startIndex, endIndex)
}

@SinceKotlin("1.8")
@ExperimentalEncodingApi
@kotlin.internal.InlineOnly
internal actual inline fun Base64.platformEncodeToByteArray(
    source: ByteArray,
    startIndex: Int,
    endIndex: Int
): ByteArray {
    return encodeToByteArrayImpl(source, startIndex, endIndex)
}