/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.reflect.KClass

/**
 * Takes the given [block] of test code and _doesn't_ execute it.
 *
 * This keeps the code under test referenced, but doesn't actually test it until it is implemented.
 */
public actual fun todo(block: () -> Unit) {
    println("TODO at " + block)
}

/** Platform-specific construction of AssertionError with cause */
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun AssertionErrorWithCause(message: String?, cause: Throwable?): AssertionError =
    AssertionError(message, cause)


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
            asserter.fail(messagePrefix(message) + "Expected an exception of $exceptionClass to be thrown, but was $e", e)
        }
    )
}


/**
 * Provides the JS implementation of asserter
 */
internal actual fun lookupAsserter(): Asserter = DefaultWasmAsserter