/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

/**
 * A number of common helper methods for writing unit tests.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kotlin.test

import kotlin.reflect.KClass

/**
 * Takes the given [block] of test code and _doesn't_ execute it.
 *
 * This keeps the code under test referenced, but doesn't actually test it until it is implemented.
 */
@Suppress("UNUSED_PARAMETER")
public actual inline fun todo(block: () -> Unit) {
    println("TODO")
}

@PublishedApi
internal actual fun <T : Throwable> checkResultIsFailure(exceptionClass: KClass<T>, message: String?, blockResult: Result<Unit>): T {
    blockResult.fold(
            onSuccess = {
                asserter.fail(messagePrefix(message) + "Expected an exception of ${exceptionClass.qualifiedName} to be thrown, but was completed successfully.")
            },
            onFailure = { e ->
                if (exceptionClass.isInstance(e)) {
                    @Suppress("UNCHECKED_CAST")
                    return e as T
                }
                asserter.fail(messagePrefix(message) + "Expected an exception of ${exceptionClass.qualifiedName} to be thrown, but was $e")
            }
    )
}

internal actual fun lookupAsserter(): Asserter = DefaultAsserter