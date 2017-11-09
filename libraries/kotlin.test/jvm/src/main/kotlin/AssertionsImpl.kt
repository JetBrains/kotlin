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

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AssertionsKt")
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
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

        @Suppress("INVISIBLE_MEMBER")
        asserter.fail(messagePrefix(message) + "Expected an exception of $exceptionClass to be thrown, but was $e")
    }

    @Suppress("INVISIBLE_MEMBER")
    val msg = messagePrefix(message)
    asserter.fail(msg + "Expected an exception of $exceptionClass to be thrown, but was completed successfully.")
}


/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
actual fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T = assertFailsWithImpl(exceptionClass.java, message, block)


/**
 * Comments out a [block] of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
@InlineOnly
actual inline fun todo(@Suppress("UNUSED_PARAMETER") block: () -> Unit) {
    System.out.println("TODO at " + currentStackTrace()[0])
}

/**
 * Returns an array of stack trace elements, each representing one stack frame.
 * The first element of the array (assuming the array is not empty) represents the top of the
 * stack, which is the place where [currentStackTrace] function was called from.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@InlineOnly
inline fun currentStackTrace() = (java.lang.Exception() as java.lang.Throwable).stackTrace

