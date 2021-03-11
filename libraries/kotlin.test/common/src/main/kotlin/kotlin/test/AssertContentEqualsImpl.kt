/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.contracts.contract

internal fun <T> assertIterableContentEquals(
    typeName: String,
    message: String?,
    expected: T?,
    actual: T?,
    iterator: T.() -> Iterator<*>
) {
    if (checkReferenceAndNullEquality(typeName, message, expected, actual, Any?::toString)) return

    var index = 0
    val expectedIt = expected.iterator()
    val actualIt = actual.iterator()

    while (expectedIt.hasNext() && actualIt.hasNext()) {
        val expectedElement = expectedIt.next()
        val actualElement = actualIt.next()

        if (expectedElement != actualElement) {
            fail(messagePrefix(message) + elementsDifferMessage(typeName, index, expectedElement, actualElement))
        }

        index++
    }

    if (expectedIt.hasNext()) {
        check(!actualIt.hasNext())

        fail(messagePrefix(message) + "$typeName lengths differ. Expected length is bigger than $index, actual length is $index.")
    }

    if (actualIt.hasNext()) {
        check(!expectedIt.hasNext())

        fail(messagePrefix(message) + "$typeName lengths differ. Expected length is $index, actual length is bigger than $index.")
    }
}

internal fun <T> assertArrayContentEquals(
    message: String?,
    expected: T?,
    actual: T?,
    size: (T) -> Int,
    get: T.(Int) -> Any?,
    contentToString: T?.() -> String,
    contentEquals: T?.(T?) -> Boolean
) {
    if (expected.contentEquals(actual)) return

    val typeName = "Array"

    if (checkReferenceAndNullEquality(typeName, message, expected, actual, contentToString)) return

    val expectedSize = size(expected)
    val actualSize = size(actual)

    if (expectedSize != actualSize) {
        val sizesDifferMessage = "$typeName sizes differ. Expected size is $expectedSize, actual size is $actualSize."
        val toString = "Expected <${expected.contentToString()}>, actual <${actual.contentToString()}>."

        fail(messagePrefix(message) + sizesDifferMessage + "\n" + toString)
    }

    for (index in 0 until expectedSize) {
        val expectedElement = expected.get(index)
        val actualElement = actual.get(index)

        if (expectedElement != actualElement) {
            val elementsDifferMessage = elementsDifferMessage(typeName, index, expectedElement, actualElement)
            val toString = "Expected <${expected.contentToString()}>, actual <${actual.contentToString()}>."

            fail(messagePrefix(message) + elementsDifferMessage + "\n" + toString)
        }
    }
}

private fun <T> checkReferenceAndNullEquality(
    typeName: String,
    message: String?,
    expected: T?,
    actual: T?,
    contentToString: T?.() -> String
): Boolean {
    contract {
        returns(false) implies (expected != null && actual != null)
    }

    if (expected === actual) {
        return true
    }
    if (expected == null) {
        fail(messagePrefix(message) + "Expected <null> $typeName, actual <${actual.contentToString()}>.")
    }
    if (actual == null) {
        fail(messagePrefix(message) + "Expected non-null $typeName <${expected.contentToString()}>, actual <null>.")
    }

    return false
}

private fun elementsDifferMessage(typeName: String, index: Int, expectedElement: Any?, actualElement: Any?): String =
    "$typeName elements differ at index $index. Expected element <$expectedElement>, actual element <${actualElement}>."