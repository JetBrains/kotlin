/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package runtime.text.utf8

import kotlin.test.*
import kotlin.reflect.KClass
import kotlinx.cinterop.toKString

// -------------------------------------- Utils --------------------------------------
fun assertEquals(expected: ByteArray, actual: ByteArray, message: String) =
        assertTrue(expected.contentEquals(actual), message)


fun checkUtf16to8(string: String, expected: IntArray, conversion: String.() -> ByteArray) {
    expected.forEach {
        assertTrue(it in Byte.MIN_VALUE..Byte.MAX_VALUE, "Expected array contains illegal values")
    }
    val expectedBytes = ByteArray(expected.size) { i -> expected[i].toByte() }
    val actual = string.conversion()
    assertEquals(expectedBytes, actual, """
        Assert failed for string: $string
        Expected: ${expected.joinToString()}
        Actual: ${actual.joinToString()}
    """.trimIndent())
}

// Utils for checking successful UTF-16 to UTF-8 conversion.
fun checkUtf16to8Replacing(string: String, expected: IntArray) {
    checkUtf16to8(string, expected) { encodeToByteArray() }
}
fun checkUtf16to8Throwing(string: String, expected: IntArray) {
    checkUtf16to8(string, expected) { encodeToByteArray(throwOnInvalidSequence = true) }
}
fun checkValidUtf16to8(string: String, expected: IntArray) {
    checkUtf16to8Replacing(string, expected)
    checkUtf16to8Throwing(string, expected)
}

fun checkUtf16to8Replacing(string: String, expected: IntArray, start: Int, size: Int) {
    checkUtf16to8(string, expected) { encodeToByteArray(start, start + size) }
}
fun checkUtf16to8Throwing(string: String, expected: IntArray, start: Int, size: Int) {
    checkUtf16to8(string, expected) { encodeToByteArray(start, start + size, true) }
}
fun checkValidUtf16to8(string: String, expected: IntArray, start: Int, size: Int) {
    checkUtf16to8Replacing(string, expected, start, size)
    checkUtf16to8Throwing(string, expected, start, size)
}


// Utils for checking successful UTF-8 to UTF-16 conversion.
fun checkUtf8to16(expected: String, array: IntArray, conversion: ByteArray.() -> String) {
    array.forEach {
        assertTrue(it in Byte.MIN_VALUE..Byte.MAX_VALUE, "Expected array contains illegal values")
    }
    val arrayBytes = ByteArray(array.size) { i -> array[i].toByte() }
    val actual = arrayBytes.conversion()
    assertEquals(expected, actual, """
        Assert failed for string: $expected
        Expected: $expected
        Actual: $actual
    """.trimIndent())
}

fun checkZeroTerminatedUtf8to16Replacing(expected: String, array: IntArray) {
    checkUtf8to16(expected, array) { toKString() }
    checkUtf8to16(expected, array.copyOf(array.size + 1)) { toKString() }
}
fun checkUtf8to16Replacing(expected: String, array: IntArray) {
    checkUtf8to16(expected, array) { decodeToString() }
    checkZeroTerminatedUtf8to16Replacing(expected, array)
}
fun checkZeroTerminatedUtf8to16Throwing(expected: String, array: IntArray) {
    checkUtf8to16(expected, array) { toKString(throwOnInvalidSequence = true) }
    checkUtf8to16(expected, array.copyOf(array.size + 1)) { toKString(throwOnInvalidSequence = true) }
}
fun checkUtf8to16Throwing(expected: String, array: IntArray) {
    checkUtf8to16(expected, array) { decodeToString(throwOnInvalidSequence = true) }
    checkZeroTerminatedUtf8to16Throwing(expected, array)
}
fun checkValidUtf8to16(expected: String, array: IntArray) {
    checkUtf8to16Replacing(expected, array)
    checkUtf8to16Throwing(expected, array)
}
fun checkValidZeroTerminatedUtf8to16(expected: String, array: IntArray) {
    checkZeroTerminatedUtf8to16Replacing(expected, array)
    checkZeroTerminatedUtf8to16Throwing(expected, array)
}

fun checkZeroTerminatedUtf8to16Replacing(expected: String, array: IntArray, start: Int, size: Int) {
    checkUtf8to16(expected, array) { toKString(start, start + size) }
    checkUtf8to16(expected, array.copyOf(array.size + 1)) { toKString(start, start + size) }
}
fun checkUtf8to16Replacing(expected: String, array: IntArray, start: Int, size: Int) {
    checkUtf8to16(expected, array) { decodeToString(start, start + size) }
    checkZeroTerminatedUtf8to16Replacing(expected, array, start, size)
}
fun checkZeroTerminatedUtf8to16Throwing(expected: String, array: IntArray, start: Int, size: Int) {
    checkUtf8to16(expected, array) { toKString(start, start + size, true) }
    checkUtf8to16(expected, array.copyOf(array.size + 1)) { toKString(start, start + size, true) }
}
fun checkUtf8to16Throwing(expected: String, array: IntArray, start: Int, size: Int) {
    checkUtf8to16(expected, array) { decodeToString(start, start + size, true) }
    checkZeroTerminatedUtf8to16Throwing(expected, array, start, size)
}
fun checkValidUtf8to16(expected: String, array: IntArray, start: Int, size: Int) {
    checkUtf8to16Replacing(expected, array, start, size)
    checkUtf8to16Throwing(expected, array, start, size)
}
fun checkValidZeroTerminatedUtf8to16(expected: String, array: IntArray, start: Int, size: Int) {
    checkZeroTerminatedUtf8to16Replacing(expected, array, start, size)
    checkZeroTerminatedUtf8to16Throwing(expected, array, start, size)
}


// Utils for checking malformed UTF-16 to UTF-8 conversion.
fun <T: Any> checkThrows(e: KClass<T>, string: String, action: () -> Unit) {
    var exception: Throwable? = null
    try {
        action()
    } catch (e: Throwable) {
        exception = e
    }
    assertNotNull(exception, "No exception was thrown for string: $string")
    assertTrue(e.isInstance(exception),"""
                Wrong exception was thrown for string: $string
                Expected: ${e.qualifiedName}
                Actual: ${exception::class.qualifiedName}: $exception}
    """.trimIndent())
}

fun checkUtf16to8Throws(string: String) {
    checkThrows(CharacterCodingException::class, string) { string.encodeToByteArray(throwOnInvalidSequence = true) }
}
fun checkUtf16to8Throws(string: String, start: Int, size: Int) {
    checkThrows(CharacterCodingException::class, string) { string.encodeToByteArray(start, start + size, true) }
}


// Utils for checking malformed UTF-8 to UTF-16 conversion.
fun <T: Any> checkUtf8to16Throws(e: KClass<T>, string: String, array: IntArray, conversion: ByteArray.() -> String)
        = checkThrows(e, string) {
    array.forEach {
        assertTrue(it in Byte.MIN_VALUE..Byte.MAX_VALUE, "Expected array contains illegal values")
    }
    val arrayBytes = ByteArray(array.size) { i -> array[i].toByte() }
    arrayBytes.conversion()
}

fun checkZeroTerminatedUtf8to16Throws(string: String, array: IntArray) {
    checkUtf8to16Throws(CharacterCodingException::class, string, array) { toKString(throwOnInvalidSequence = true) }
    checkUtf8to16Throws(CharacterCodingException::class, string, array.copyOf(array.size + 1)) { toKString(throwOnInvalidSequence = true) }
}
fun checkUtf8to16Throws(string: String, array: IntArray) {
    checkUtf8to16Throws(CharacterCodingException::class, string, array) { decodeToString(throwOnInvalidSequence = true) }
    checkZeroTerminatedUtf8to16Throws(string, array)
}
fun checkZeroTerminatedUtf8to16Throws(string: String, array: IntArray, start: Int, size: Int) {
    checkUtf8to16Throws(CharacterCodingException::class, string, array) { toKString(start, start + size, true) }
    checkUtf8to16Throws(CharacterCodingException::class, string, array.copyOf(array.size + 1)) { toKString(start, start + size, true) }
}
fun checkUtf8to16Throws(string: String, array: IntArray, start: Int, size: Int) {
    checkUtf8to16Throws(CharacterCodingException::class, string, array) { decodeToString(start, start + size, true) }
    checkZeroTerminatedUtf8to16Throws(string, array, start, size)
}


// Utils for checking String to floating-point number conversion.
fun checkDoubleConversionThrows(string: String) = checkThrows(NumberFormatException::class, string) {
    string.toDouble()
}

fun checkFloatConversionThrows(string: String) = checkThrows(NumberFormatException::class, string) {
    string.toFloat()
}


// Utils for checking invalid-bounds-exception thrown by UTF-16 to UTF-8 conversion.
fun <T: Any> checkOutOfBoundsUtf16to8Replacing(e: KClass<T>, string: String, start: Int, size: Int) {
    checkThrows(e, string) { string.encodeToByteArray(start, start + size) }
}
fun <T: Any> checkOutOfBoundsUtf16to8Throwing(e: KClass<T>, string: String, start: Int, size: Int) {
    checkThrows(e, string) { string.encodeToByteArray(start, start + size, true) }
}
fun <T: Any> checkOutOfBoundsUtf16to8(e: KClass<T>, string: String, start: Int, size: Int) {
    checkOutOfBoundsUtf16to8Replacing(e, string, start, size)
    checkOutOfBoundsUtf16to8Throwing(e, string, start, size)
}


// Utils for checking invalid-bounds-exception thrown by UTF-8 to UTF-16 conversion.
fun <T: Any> checkOutOfBoundsZeroTerminatedUtf8to16Replacing(e: KClass<T>, string: String, byteArray: ByteArray, start: Int, size: Int) {
    checkThrows(e, string) { byteArray.toKString(start, start + size) }
}
fun <T: Any> checkOutOfBoundsUtf8to16Replacing(e: KClass<T>, string: String, byteArray: ByteArray, start: Int, size: Int) {
    checkThrows(e, string) { byteArray.decodeToString(start, start + size) }
    checkOutOfBoundsZeroTerminatedUtf8to16Replacing(e, string, byteArray, start, size)
}
fun <T: Any> checkOutOfBoundsZeroTerminatedUtf8to16Throwing(e: KClass<T>, string: String, byteArray: ByteArray, start: Int, size: Int) {
    checkThrows(e, string) { byteArray.toKString(start, start + size, true) }
}
fun <T: Any> checkOutOfBoundsUtf8to16Throwing(e: KClass<T>, string: String, byteArray: ByteArray, start: Int, size: Int) {
    checkThrows(e, string) { byteArray.decodeToString(start, start + size, true) }
    checkOutOfBoundsZeroTerminatedUtf8to16Throwing(e, string, byteArray, start, size)
}
fun <T: Any> checkOutOfBoundsUtf8to16(e: KClass<T>, string: String, byteArray: ByteArray, start: Int, size: Int) {
    checkOutOfBoundsUtf8to16Replacing(e, string, byteArray, start, size)
    checkOutOfBoundsUtf8to16Throwing(e, string, byteArray, start, size)
}
fun <T: Any> checkOutOfBoundsZeroTerminatedUtf8to16(e: KClass<T>, string: String, byteArray: ByteArray, start: Int, size: Int) {
    checkOutOfBoundsZeroTerminatedUtf8to16Replacing(e, string, byteArray, start, size)
    checkOutOfBoundsZeroTerminatedUtf8to16Throwing(e, string, byteArray, start, size)
}


// Util for performing action on result of UTF-8 to UTF-16 conversion.
fun convertUtf8to16(byteArray: ByteArray, action: (String) -> Unit) {
    byteArray.decodeToString().let { action(it) }
    byteArray.toKString().let { action(it) }
}

// ------------------------- Test UTF-16 to UTF-8 conversion -------------------------
fun test16to8() {
    // Valid strings.
    checkValidUtf16to8("Hello", intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
    checkValidUtf16to8("Привет", intArrayOf(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
    checkValidUtf16to8("\uD800\uDC00", intArrayOf(-16, -112, -128, -128))
    checkValidUtf16to8("", intArrayOf())

    // Test manual conversion with replacement.
    // Illegal surrogate pair -> replace with default
    checkUtf16to8Replacing("\uDC00\uD800", intArrayOf(-17, -65, -67, -17, -65, -67))
    // Different kinds of input
    checkUtf16to8Replacing("\uD800\uDC001\uDC00\uD800",
            intArrayOf(-16, -112, -128, -128, '1'.toInt(), -17, -65, -67, -17, -65, -67))
    // Lone surrogate - replace with default
    checkUtf16to8Replacing("\uD80012", intArrayOf(-17, -65, -67, '1'.toInt(), '2'.toInt()))
    checkUtf16to8Replacing("\uDC0012", intArrayOf(-17, -65, -67, '1'.toInt(), '2'.toInt()))
    checkUtf16to8Replacing("12\uD800", intArrayOf('1'.toInt(), '2'.toInt(), -17, -65, -67))

    // Test manual conversion with an exception if an input is invalid.
    // Illegal surrogate pair -> throw
    checkUtf16to8Throws("\uDC00\uD800")
    // Different kinds of input (including illegal one) -> throw
    checkUtf16to8Throws("\uD800\uDC001\uDC00\uD800")
    // Lone surrogate - throw
    checkUtf16to8Throws("\uD80012")
    checkUtf16to8Throws("\uDC0012")
    checkUtf16to8Throws("12\uD800")

    // Test double parsing.
    assertEquals(4.2, "4.2".toDouble())
    // Illegal surrogate pair -> throw
    checkDoubleConversionThrows("\uDC00\uD800")
    // Different kinds of input (including illegal one) -> throw
    checkDoubleConversionThrows("\uD800\uDC001\uDC00\uD800")
    // Lone surrogate - throw
    checkDoubleConversionThrows("\uD80012")
    checkDoubleConversionThrows("\uDC0012")
    checkDoubleConversionThrows("12\uD800")

    // Test float parsing.
    assertEquals(4.2F,  "4.2".toFloat())
    // Illegal surrogate pair -> throw
    checkFloatConversionThrows("\uDC00\uD800")
    // Different kinds of input (including illegal one) -> throw
    checkFloatConversionThrows("\uD800\uDC001\uDC00\uD800")
    // Lone surrogate - throw
    checkFloatConversionThrows("\uD80012")
    checkFloatConversionThrows("\uDC0012")
    checkFloatConversionThrows("12\uD800")
}

fun test16to8CustomBorders() {
    // Valid strings.
    checkValidUtf16to8("Hello!", intArrayOf('H'.toInt(), 'e'.toInt()), 0, 2)
    checkValidUtf16to8("Hello!", intArrayOf('e'.toInt(), 'l'.toInt()), 1, 2)
    checkValidUtf16to8("Hello!", intArrayOf('o'.toInt(), '!'.toInt()), 4, 2)
    checkValidUtf16to8("Hello!", intArrayOf(), 0, 0)
    checkValidUtf16to8("Hello!", intArrayOf(), 6, 0)

    checkValidUtf16to8("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 0, 4)
    checkValidUtf16to8("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 2, 4)
    checkValidUtf16to8("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 4, 4)

    // Index out of bound
    checkOutOfBoundsUtf16to8(IndexOutOfBoundsException::class, "Hello", -1, 4)
    checkOutOfBoundsUtf16to8(IndexOutOfBoundsException::class, "Hello", 5, 10)
    checkOutOfBoundsUtf16to8(IndexOutOfBoundsException::class, "Hello", 2, 10)
    checkOutOfBoundsUtf16to8(IllegalArgumentException::class, "Hello", 3, -2)

    // Test manual conversion with replacement and custom borders.
    // Illegal surrogate pair -> replace with default
    checkUtf16to8Replacing("\uDC00\uD80012",
            intArrayOf(-17, -65, -67, -17, -65, -67, '1'.toInt()), 0, 3)
    checkUtf16to8Replacing("1\uDC00\uD8002",
            intArrayOf(-17, -65, -67, -17, -65, -67, '2'.toInt()), 1, 3)
    checkUtf16to8Replacing("12\uDC00\uD800",
            intArrayOf('2'.toInt(), -17, -65, -67, -17, -65, -67), 1, 3)

    // Lone surrogate - replace with default
    checkUtf16to8Replacing("1\uD800\uDC002", intArrayOf('1'.toInt(), -17, -65, -67), 0, 2)
    checkUtf16to8Replacing("1\uD800\uDC002", intArrayOf(-17, -65, -67, '2'.toInt()), 2, 2)

    // Test manual conversion with an exception if an input is invalid and custom borders.
    // Illegal surrogate pair -> throw
    checkUtf16to8Throws("\uDC00\uD80012", 0, 3)
    checkUtf16to8Throws("1\uDC00\uD8002", 1, 3)
    checkUtf16to8Throws("12\uDC00\uD800", 1, 3)

    // Lone surrogate -> throw
    checkUtf16to8Throws("1\uD800\uDC002", 0, 2)
    checkUtf16to8Throws("1\uD800\uDC002",  2, 2)
}

fun testPrint() {
    // Valid strings.
    println("Hello")
    println("Привет")
    println("\uD800\uDC00")
    println("")

    // Illegal surrogate pair -> default output
    println("\uDC00\uD800")
    // Lone surrogate -> default output
    println("\uD80012")
    println("\uDC0012")
    println("12\uD800")

    // https://github.com/JetBrains/kotlin-native/issues/1091
    val array = byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x98.toByte(), 0xA5.toByte())
    convertUtf8to16(array) { badString ->
        assertEquals(2, badString.length)
        println(badString)
    }
}

// ------------------------- Test UTF-8 to UTF-16 conversion -------------------------
fun test8to16() {
    // Valid strings.
    checkValidUtf8to16("Hello", intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
    checkValidUtf8to16("Привет", intArrayOf(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
    checkValidUtf8to16("\uD800\uDC00", intArrayOf(-16, -112, -128, -128))
    checkValidUtf8to16("", intArrayOf())

    // Test manual conversion with replacement.
    // Incorrect UTF-8 lead character.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-1, '1'.toInt()))

    // Incomplete codepoint.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt()))
    checkUtf8to16Replacing("\uFFFD1\uFFFD", intArrayOf(-16, -97, -104, '1'.toInt(), -16, -97, -104))

    // Test manual conversion with exception throwing
    // Incorrect UTF-8 lead character -> throw.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-1, '1'.toInt()))

    // Incomplete codepoint -> throw.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt()))
    checkUtf8to16Throws("\uFFFD1\uFFFD", intArrayOf(-16, -97, -104, '1'.toInt(), -16, -97, -104))
}

fun test8to16CustomBorders() {
    // Valid strings.
    checkValidUtf8to16("He",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()),0, 2)
    checkValidUtf8to16("ll",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 2, 2)
    checkValidUtf8to16("lo",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 3, 2)
    checkValidUtf8to16("",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 0, 0)

    checkValidUtf8to16("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            0, 8)
    checkValidUtf8to16("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            4, 8)
    checkValidUtf8to16("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            8, 8)

    // Index out of bound
    val helloArray = byteArrayOf('H'.toByte(), 'e'.toByte(), 'l'.toByte(), 'l'.toByte(), 'o'.toByte())
    checkOutOfBoundsUtf8to16(IndexOutOfBoundsException::class, "Hello", helloArray, -1, 4)
    checkOutOfBoundsUtf8to16(IndexOutOfBoundsException::class, "Hello", helloArray, 5, 10)
    checkOutOfBoundsUtf8to16(IndexOutOfBoundsException::class, "Hello", helloArray, 2, 10)
    checkOutOfBoundsUtf8to16(IndexOutOfBoundsException::class, "Hello", helloArray, 10, 0)
    checkOutOfBoundsUtf8to16(IllegalArgumentException::class, "Hello", helloArray, 3, -2)

    // Test manual conversion with replacement and custom borders.
    // Incorrect UTF-8 lead character.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-1, '1'.toInt(), '2'.toInt()), 0, 2)
    checkUtf8to16Replacing("\uFFFD2", intArrayOf('1'.toInt(), -1, '2'.toInt(), '3'.toInt()), 1, 2)
    checkUtf8to16Replacing("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -1), 1, 2)

    // Incomplete codepoint.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt(), '2'.toInt()), 0, 4)
    checkUtf8to16Replacing("\uFFFD2", intArrayOf('1'.toInt(), -16, -97, -104, '2'.toInt(), '3'.toInt()), 1, 4)
    checkUtf8to16Replacing("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -16, -97, -104),  1, 4)

    // Test manual conversion with an exception if an input is invalid and custom borders.
    // Incorrect UTF-8 lead character.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-1, '1'.toInt(), '2'.toInt()), 0, 2)
    checkUtf8to16Throws("\uFFFD2", intArrayOf('1'.toInt(), -1, '2'.toInt(), '3'.toInt()), 1, 2)
    checkUtf8to16Throws("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -1), 1, 2)

    // Incomplete codepoint.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt(), '2'.toInt()), 0, 4)
    checkUtf8to16Throws("\uFFFD2", intArrayOf('1'.toInt(), -16, -97, -104, '2'.toInt(), '3'.toInt()), 1, 4)
    checkUtf8to16Throws("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -16, -97, -104),  1, 4)
}

// ----------------- Test zero-terminated UTF-8 to UTF-16 conversion -----------------
fun testZeroTerminated8To16() {
    // Valid strings.
    checkValidZeroTerminatedUtf8to16("Hell", intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 0, 'o'.toInt()))
    checkValidZeroTerminatedUtf8to16("При", intArrayOf(-48, -97, -47, -128, -48, -72, 0, -48, -78, 0, -48, -75, -47, -126))
    checkValidZeroTerminatedUtf8to16("\uD800\uDC00", intArrayOf(-16, -112, -128, -128, 0, -16, -112, -128, -128))
    checkValidZeroTerminatedUtf8to16("", intArrayOf(0, 'H'.toInt()))

    // Test manual conversion with replacement.
    // Incorrect UTF-8 lead character.
    checkZeroTerminatedUtf8to16Replacing("\uFFFD", intArrayOf(-1, 0, '1'.toInt()))

    // Incomplete codepoint.
    checkZeroTerminatedUtf8to16Replacing("\uFFFD", intArrayOf(-16, -97, -104, 0, '1'.toInt()))
    checkZeroTerminatedUtf8to16Replacing("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt(), 0, -16, -97, -104))

    // Test manual conversion with exception throwing
    // Incorrect UTF-8 lead character -> throw.
    checkZeroTerminatedUtf8to16Throws("\uFFFD", intArrayOf(-1, 0, '1'.toInt()))

    // Incomplete codepoint -> throw.
    checkZeroTerminatedUtf8to16Throws("\uFFFD", intArrayOf(-16, -97, -104, 0, '1'.toInt()))
    checkZeroTerminatedUtf8to16Throws("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt(), 0, -16, -97, -104))
}

fun testZeroTerminated8To16CustomBorders() {
    val array = intArrayOf('a'.toInt(), 'a'.toInt(), 'a'.toInt(), 0, 'b'.toInt(), 'b'.toInt(), 'b'.toInt(), 0)
    checkValidZeroTerminatedUtf8to16("aaa", array, 0, 5)
    checkValidZeroTerminatedUtf8to16("a", array, 2, 2)
    checkValidZeroTerminatedUtf8to16("", array, 3, 2)
    checkValidZeroTerminatedUtf8to16("bb", array, 4, 2)
    checkValidZeroTerminatedUtf8to16("bbb", array, 4, 3)
    checkValidZeroTerminatedUtf8to16("bbb", array, 4, 4)
    checkValidZeroTerminatedUtf8to16("bb", array, 5, 3)
    checkValidZeroTerminatedUtf8to16("b", array, 6, 2)
    checkValidZeroTerminatedUtf8to16("", array, 7, 1)
    checkValidZeroTerminatedUtf8to16("", array, 8, 0)

    val byteArray = ByteArray(array.size) { array[it].toByte() }
    checkOutOfBoundsZeroTerminatedUtf8to16(IndexOutOfBoundsException::class, "aaa0bbb0", byteArray, -1, 4)
    checkOutOfBoundsZeroTerminatedUtf8to16(IndexOutOfBoundsException::class, "aaa0bbb0", byteArray, 8, 10)
    checkOutOfBoundsZeroTerminatedUtf8to16(IndexOutOfBoundsException::class, "aaa0bbb0", byteArray, 2, 10)
    checkOutOfBoundsZeroTerminatedUtf8to16(IndexOutOfBoundsException::class, "aaa0bbb0", byteArray, 10, 0)
    checkOutOfBoundsZeroTerminatedUtf8to16(IllegalArgumentException::class, "aaa0bbb0", byteArray, 3, -2)
}

// ------------------------------------ Run tests ------------------------------------
@Test fun runTest() {
    test16to8()
    test16to8CustomBorders()
    test8to16()
    test8to16CustomBorders()
    testZeroTerminated8To16()
    testZeroTerminated8To16CustomBorders()
    testPrint()
}
