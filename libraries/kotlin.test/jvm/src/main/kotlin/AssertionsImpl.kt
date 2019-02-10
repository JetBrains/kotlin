/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AssertionsKt")

package kotlin.test

import kotlin.internal.*
import kotlin.reflect.*

/** Asserts that a [block] fails with a specific exception being thrown. */
private fun <T : Throwable> assertFailsWithImpl(exceptionClass: Class<T>, message: String?, block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (exceptionClass.isInstance(e)) {
            @Suppress("UNCHECKED_CAST")
            return e as T
        }

        asserter.fail(messagePrefix(message) + "Expected an exception of $exceptionClass to be thrown, but was $e")
    }

    val msg = messagePrefix(message)
    asserter.fail(msg + "Expected an exception of $exceptionClass to be thrown, but was completed successfully.")
}


/**
 * Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown.
 *
 * If the assertion fails, the specified [message] is used unless it is null as a prefix for the failure message.
 *
 * @return An exception of the expected exception type [T] that successfully caught.
 * The returned exception can be inspected further, for example by asserting its property values.
 */
actual fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T =
    assertFailsWithImpl(exceptionClass.java, message, block)


/**
 * Takes the given [block] of test code and _doesn't_ execute it.
 *
 * This keeps the code under test referenced, but doesn't actually test it until it is implemented.
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@InlineOnly
actual inline fun todo(@Suppress("UNUSED_PARAMETER") block: () -> Unit) {
    println("TODO at " + currentStackTrace()[0])
}

/**
 * Returns an array of stack trace elements, each representing one stack frame.
 * The first element of the array (assuming the array is not empty) represents the top of the
 * stack, which is the place where [currentStackTrace] function was called from.
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@InlineOnly
inline fun currentStackTrace() = @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") (java.lang.Exception() as java.lang.Throwable).stackTrace

