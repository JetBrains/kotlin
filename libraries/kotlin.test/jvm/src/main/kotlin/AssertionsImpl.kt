/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AssertionsKt")

package kotlin.test

import kotlin.internal.*
import kotlin.reflect.*

/** Asserts that a [blockResult] is a failure with the specific exception type being thrown. */
@PublishedApi
internal actual fun <T : Throwable> checkResultIsFailure(exceptionClass: KClass<T>, message: String?, blockResult: Result<Any?>): T {
    blockResult.fold(
        onSuccess = { v ->
            val msg = messagePrefix(message)
            asserter.fail(msg + "Expected an exception of ${exceptionClass.java} to be thrown, ${formatResultMessage(v)}")
        },
        onFailure = { e ->
            if (exceptionClass.java.isInstance(e)) {
                @Suppress("UNCHECKED_CAST")
                return e as T
            }

            asserter.fail(messagePrefix(message) + "Expected an exception of ${exceptionClass.java} to be thrown, but was $e", e)
        }
    )
}

/**
 * Takes the given [block] of test code and _doesn't_ execute it.
 *
 * This keeps the code under test referenced, but doesn't actually test it until it is implemented.
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@InlineOnly
public actual inline fun todo(@Suppress("UNUSED_PARAMETER") block: () -> Unit) {
    println("TODO at " + currentStackTrace()[0])
}

/**
 * Returns an array of stack trace elements, each representing one stack frame.
 * The first element of the array (assuming the array is not empty) represents the top of the
 * stack, which is the place where [currentStackTrace] function was called from.
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@InlineOnly
public inline fun currentStackTrace(): Array<StackTraceElement> = @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") (java.lang.Exception() as java.lang.Throwable).stackTrace


/** Platform-specific construction of AssertionError with cause */
internal actual fun AssertionErrorWithCause(message: String?, cause: Throwable?): AssertionError {
    val assertionError = if (message == null) AssertionError() else AssertionError(message)
    assertionError.initCause(cause)
    return assertionError
}
