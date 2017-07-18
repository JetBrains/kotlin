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

/**
 * Describes the result of an assertion execution.
 */
public external interface AssertionResult {
    val result: Boolean
    val expected: Any?
    val actual: Any?
    val lazyMessage: () -> String?
}

internal var assertHook: (AssertionResult) -> Unit = { _ -> }

internal object DefaultJsAsserter : Asserter {
    private var e: Any? = undefined
    private var a: Any? = undefined

    override fun assertEquals(message: String?, expected: Any?, actual: Any?) {
        e = expected
        a = actual
        super.assertEquals(message, expected, actual)
    }

    override fun assertNotEquals(message: String?, illegal: Any?, actual: Any?) {
        e = illegal
        a = actual
        super.assertNotEquals(message, illegal, actual)
    }

    override fun assertNull(message: String?, actual: Any?) {
        a = actual
        super.assertNull(message, actual)
    }

    override fun assertNotNull(message: String?, actual: Any?) {
        a = actual
        super.assertNotNull(message, actual)
    }

    override fun assertTrue(lazyMessage: () -> String?, actual: Boolean) {
        if (!actual) {
            failWithMessage(lazyMessage)
        }
        else {
            invokeHook(true, lazyMessage)
        }
    }

    override fun assertTrue(message: String?, actual: Boolean) {
        assertTrue({ message }, actual)
    }

    override fun fail(message: String?): Nothing {
        failWithMessage { message }
    }

    private fun failWithMessage(lazyMessage: () -> String?): Nothing {
        val message = lazyMessage()
        invokeHook(false) { message }
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }

    private fun invokeHook(result: Boolean, lazyMessage: () -> String?) {
        try {
            assertHook(object : AssertionResult {
                override val result: Boolean = result
                override val expected: Any? = e
                override val actual: Any? = a
                override val lazyMessage: () -> String? = lazyMessage
            })
        }
        finally {
            e = undefined
            a = undefined
        }
    }
}