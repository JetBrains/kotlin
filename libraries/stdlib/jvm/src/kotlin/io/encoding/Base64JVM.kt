/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.encoding

@SinceKotlin("1.8")
@ExperimentalEncodingApi
@kotlin.internal.InlineOnly
internal actual inline fun Base64.platformCharsToBytes(source: CharSequence, startIndex: Int, endIndex: Int): ByteArray {
    return if (source is String) {
        checkSourceBounds(source.length, startIndex, endIndex)
        // up to 10x faster than the Common implementation
        source.substring(startIndex, endIndex).toByteArray(Charsets.ISO_8859_1)
    } else {
        charsToBytesImpl(source, startIndex, endIndex)
    }
}


@SinceKotlin("1.8")
@ExperimentalEncodingApi
@kotlin.internal.InlineOnly
internal actual inline fun Base64.platformEncodeToString(source: ByteArray, startIndex: Int, endIndex: Int): String {
//    val subArray = if (startIndex == 0 && endIndex == source.size) {
//        source
//    } else {
//        source.copyOfRange(startIndex, endIndex)
//    }
//    return javaEncoder().encodeToString(subArray)
    // TODO: Move to kotlin-stdlib-jdk8 and use the commented-out implementation above when KT-54970 gets fixed.
    val byteResult = encodeToByteArrayImpl(source, startIndex, endIndex)
    return String(byteResult, Charsets.ISO_8859_1)
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
//    return if (destinationOffset == 0 && startIndex == 0 && endIndex == source.size) {
//        // up to 2x faster than the Common implementation
//        javaEncoder().encode(source, destination)
//    } else {
//        encodeIntoByteArrayImpl(source, destination, destinationOffset, startIndex, endIndex)
//    }
    // TODO: Move to kotlin-stdlib-jdk8 and use the commented-out implementation above when KT-54970 gets fixed.
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
//    return if (startIndex == 0 && endIndex == source.size) {
//        // up to 2x faster than the Common implementation
//        javaEncoder().encode(source)
//    } else {
//        encodeToByteArrayImpl(source, startIndex, endIndex)
//    }
    // TODO: Move to kotlin-stdlib-jdk8 and use the commented-out implementation above when KT-54970 gets fixed.
    return encodeToByteArrayImpl(source, startIndex, endIndex)
}

//@SinceKotlin("1.8")
//@ExperimentalEncodingApi
//private fun Base64.javaEncoder(): java.util.Base64.Encoder {
//    return if (isMimeScheme) {
//        java.util.Base64.getMimeEncoder(Base64.mimeLineLength, Base64.mimeLineSeparatorSymbols)
//    } else if (isUrlSafe) {
//        java.util.Base64.getUrlEncoder()
//    } else {
//        java.util.Base64.getEncoder()
//    }
//}