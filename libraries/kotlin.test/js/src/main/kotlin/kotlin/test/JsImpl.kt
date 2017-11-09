/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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