/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.reflect.KClass

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
actual fun todo(block: () -> Unit) {
    // println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
    println("TODO at " + block)
}


/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
actual fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T {
    val exception = assertFails(message, block)
    @Suppress("INVISIBLE_MEMBER")
    assertTrue(exceptionClass.isInstance(exception), messagePrefix(message) + "Expected an exception of $exceptionClass to be thrown, but was $exception")

    @Suppress("UNCHECKED_CAST")
    return exception as T
}


/**
 * Provides the JS implementation of asserter
 */
internal actual fun lookupAsserter(): Asserter = DefaultJsAsserter