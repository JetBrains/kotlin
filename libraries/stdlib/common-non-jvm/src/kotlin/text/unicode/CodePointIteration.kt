/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.unicode

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun String.codePointAt(index: Int): CodePoint = (this as CharSequence).codePointAt(index)


@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharSequence.codePointAt(index: Int): CodePoint {
    if (index !in 0..<length) throw IndexOutOfBoundsException("index: $index is required to be in range 0..<$length")

    return codePointAtImpl(index, length, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharArray.codePointAt(index: Int): CodePoint {
    if (index !in 0..<size) throw IndexOutOfBoundsException("index: $index is required to be in range 0..<$size")

    return codePointAtImpl(index, size, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharArray.codePointAt(index: Int, endIndex: Int): CodePoint {
    if (endIndex !in 0..size) throw IndexOutOfBoundsException("endIndex: $endIndex is required to be in range 0..$size")
    if (index !in 0..<endIndex) throw IndexOutOfBoundsException("index: $index is required to be in range 0..<$endIndex")

    return codePointAtImpl(index, endIndex, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
private inline fun codePointAtImpl(index: Int, endIndex: Int, charAt: (Int) -> Char): CodePoint {
    val c1 = charAt(index)
    if (index < endIndex - 1 && c1.isHighSurrogate()) {
        val c2 = charAt(index + 1)
        if (c2.isLowSurrogate()) return CodePoint.fromSurrogatePairUnchecked(c1, c2)
    }
    return c1.toCodePoint()
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun String.codePointBefore(index: Int): CodePoint = (this as CharSequence).codePointBefore(index)

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharSequence.codePointBefore(index: Int): CodePoint {
    if (index !in 1..length) throw IndexOutOfBoundsException("index: $index is required to be in range 1..$length")

    return codePointBeforeImpl(index, 0, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharArray.codePointBefore(index: Int): CodePoint {
    if (index !in 1..size) throw IndexOutOfBoundsException("index: $index is required to be in range 1..$size")

    return codePointBeforeImpl(index, 0, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharArray.codePointBefore(index: Int, startIndex: Int): CodePoint {
    if (startIndex !in 0..size) throw IndexOutOfBoundsException("startIndex: $startIndex is required to be in range 0..$size")
    if (index !in startIndex + 1..size) throw IndexOutOfBoundsException("index: $index is required to be in range ${startIndex + 1}..$size")

    return codePointBeforeImpl(index, startIndex, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
private inline fun codePointBeforeImpl(index: Int, startIndex: Int, charAt: (Int) -> Char): CodePoint {
    val c2 = charAt(index - 1)
    if (index > startIndex + 1 && c2.isLowSurrogate()) {
        val c1 = charAt(index - 2)
        if (c1.isHighSurrogate()) return CodePoint.fromSurrogatePairUnchecked(c1, c2)
    }
    return c2.toCodePoint()
}


@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun String.codePointCount(startIndex: Int, endIndex: Int): Int {
    if (startIndex < 0 || endIndex > length || startIndex > endIndex) throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, length: $length")

    return codePointCountImpl(startIndex, endIndex, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharSequence.codePointCount(startIndex: Int, endIndex: Int): Int {
    if (startIndex < 0 || endIndex > length || startIndex > endIndex) throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, length: $length")

    return codePointCountImpl(startIndex, endIndex, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharArray.codePointCount(startIndex: Int, endIndex: Int): Int {
    if (startIndex < 0 || endIndex > size || startIndex > endIndex) throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")

    return codePointCountImpl(startIndex, endIndex, this::get)
}

private inline fun codePointCountImpl(startIndex: Int, endIndex: Int, charAt: (Int) -> Char): Int {
    var index = startIndex
    var count = 0
    while (index < endIndex) {
        val c1 = charAt(index)
        index++
        if (c1.isHighSurrogate() && index < endIndex) {
            val c2 = charAt(index)
            if (c2.isLowSurrogate()) {
                index++
            }
        }
        count++
    }

    return count
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun String.offsetByCodePoints(index: Int, codePointOffset: Int): Int {
    if (index !in 0..length) throw IndexOutOfBoundsException("index: $index, length: $length")

    return offsetByCodePointsImpl(index, codePointOffset, 0, length, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharSequence.offsetByCodePoints(index: Int, codePointOffset: Int): Int {
    if (index !in 0..length) throw IndexOutOfBoundsException("index: $index, length: $length")

    return offsetByCodePointsImpl(index, codePointOffset, 0, length, this::get)
}

@SinceKotlin("2.5")
@ExperimentalUnicodeApi
public actual fun CharArray.offsetByCodePoints(index: Int, codePointOffset: Int, startIndex: Int, endIndex: Int): Int {
    if (startIndex < 0 || endIndex > size || startIndex > endIndex) throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")
    if (index !in startIndex..endIndex) throw IndexOutOfBoundsException("index: $index, startIndex: $startIndex, endIndex: $endIndex")

    return offsetByCodePointsImpl(index, codePointOffset, startIndex, endIndex, this::get)
}

private inline fun offsetByCodePointsImpl(index: Int, codePointOffset: Int, startIndex: Int, endIndex: Int, charAt: (Int) -> Char): Int {
    if (codePointOffset == 0) return index

    if (codePointOffset > 0) {
        var currentIndex = index
        repeat(codePointOffset) {
            if (currentIndex >= endIndex) throw IndexOutOfBoundsException("index: $index, offset: $codePointOffset, startIndex: $startIndex, endIndex: $index")
            val firstChar = charAt(currentIndex)
            currentIndex++
            if (firstChar.isHighSurrogate() && currentIndex < endIndex) {
                val nextChar = charAt(currentIndex)
                if (nextChar.isLowSurrogate()) {
                    currentIndex++
                }
            }
        }

        return currentIndex
    } else {
        var currentIndex = index - 1
        repeat(-codePointOffset) {
            if (currentIndex < startIndex) throw IndexOutOfBoundsException("index: $index, offset: $codePointOffset, startIndex: $startIndex, endIndex: $index")
            val c2 = charAt(currentIndex)
            currentIndex--
            if (c2.isLowSurrogate() && currentIndex >= startIndex) {
                val c1 = charAt(currentIndex)
                if (c1.isHighSurrogate()) {
                    currentIndex--
                }
            }
        }

        return currentIndex + 1
    }
}
