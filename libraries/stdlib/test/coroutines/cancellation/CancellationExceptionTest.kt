/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package coroutines.cancellation

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals

class CancellationExceptionTest {

    @Test
    fun testAllConstructors() {
        // Mostly test NO_ACTUAL_WITHOUT_EXPECT though
        val cause = ArithmeticException()
        val message = "message"
        checkException(CancellationException(message, cause), cause, message)
        checkException(CancellationException(message, null), null, message)
        checkException(CancellationException(cause), cause, cause.defaultMessage())
        checkException(CancellationException(message), null, message)
        checkException(CancellationException(null, cause), cause, null)
        checkException(CancellationException(cause = cause), cause, cause.defaultMessage())
        // does not work on JVM because of typealias
//      checkException(CancellationException(message = message), null, message)
        checkException(CancellationException(), null, null)
    }

    private fun Throwable?.defaultMessage() = toString()

    private fun checkException(e: CancellationException, expectedCause: Throwable?, expectedMessage: String?) {
        assertEquals(expectedCause, e.cause)
        assertEquals(expectedMessage, e.message)
    }
}