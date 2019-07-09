/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.reflect.KClass

/**
 * Takes the given [block] of test code and _doesn't_ execute it.
 *
 * This keeps the code under test referenced, but doesn't actually test it until it is implemented.
 */
actual fun todo(block: () -> Unit) {
    // println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
    println("TODO at " + block)
}


@PublishedApi
internal actual fun <T : Throwable> checkResultIsFailure(exceptionClass: KClass<T>, message: String?, blockResult: Result<Unit>): T {
    blockResult.fold(
        onSuccess = {
            asserter.fail(messagePrefix(message) + "Expected an exception of $exceptionClass to be thrown, but was completed successfully.")
        },
        onFailure = { e ->
            if (exceptionClass.isInstance(e)) {
                @Suppress("UNCHECKED_CAST")
                return e as T
            }
            asserter.fail(messagePrefix(message) + "Expected an exception of $exceptionClass to be thrown, but was $e")
        }
    )
}


/**
 * Provides the JS implementation of asserter
 */
internal actual fun lookupAsserter(): Asserter = DefaultJsAsserter