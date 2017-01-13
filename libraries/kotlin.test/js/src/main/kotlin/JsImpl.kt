/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
fun todo(block: () -> Any) {
    // println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
    println("TODO at " + block)
}

/** Asserts that a [block] fails with a specific exception of type [T] being thrown.
 *  Since inline method doesn't allow to trace where it was invoked, it is required to pass a [message] to distinguish this method call from others.
 */
inline fun <reified T : Throwable> assertFailsWith(message: String? = null, noinline block: () -> Unit): T {
    val exception = assertFails(message, block)
    val messagePrefix = if (message == null) "" else "$message. "

    assertTrue(exception is T, "${messagePrefix}An exception thrown is not of the expected type: $exception")
    return exception as T
}

var _asserter: Asserter = QUnitAsserter()

/**
 * Provides the JS implementation of asserter using [QUnit](http://QUnitjs.com/)
 */
internal impl fun lookupAsserter(): Asserter = _asserter

class QUnitAsserter : Asserter {

    override fun assertTrue(lazyMessage: () -> String?, actual: Boolean) {
        assertTrue(actual, lazyMessage())
    }

    override fun assertTrue(message: String?, actual: Boolean) {
        QUnit.ok(actual, message)
        if (!actual) failWithMessage(message)
    }

    override fun fail(message: String?): Nothing {
        QUnit.ok(false, message)
        failWithMessage(message)
    }

    private fun failWithMessage(message: String?): Nothing {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}
