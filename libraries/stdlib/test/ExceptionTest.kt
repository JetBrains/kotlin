/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.exceptions

import kotlin.test.*

class ExceptionTest {
    private val cause = Exception("cause")

    @Test fun throwable() = testCreateException(::Throwable, ::Throwable, ::Throwable, ::Throwable)
    @Test fun error() = testCreateException(::Error, ::Error, ::Error, ::Error)
    @Test fun exception() = testCreateException(::Exception, ::Exception, ::Exception, ::Exception)
    @Test fun runtimeException() = testCreateException(::RuntimeException, ::RuntimeException, ::RuntimeException, ::RuntimeException)
    @Test fun illegalArgumentException() = testCreateException(::IllegalArgumentException, ::IllegalArgumentException, ::IllegalArgumentException, ::IllegalArgumentException)
    @Test fun illegalStateException() = testCreateException(::IllegalStateException, ::IllegalStateException, ::IllegalStateException, ::IllegalStateException)
    @Test fun indexOutOfBoundsException() = testCreateException(::IndexOutOfBoundsException, ::IndexOutOfBoundsException)
    @Test fun unsupportedOperationException() = testCreateException(::UnsupportedOperationException, ::UnsupportedOperationException, ::UnsupportedOperationException, ::UnsupportedOperationException)
    @Test fun numberFormatException() = testCreateException(::NumberFormatException, ::NumberFormatException)
    @Test fun nullPointerException() = testCreateException(::NullPointerException, ::NullPointerException)
    @Test fun classCastException() = testCreateException(::ClassCastException, ::ClassCastException)
    @Test fun noSuchElementException() = testCreateException(::NoSuchElementException, ::NoSuchElementException)
    @Test fun concurrentModificationException() = testCreateException(::ConcurrentModificationException, ::ConcurrentModificationException)
    @Test fun arithmeticException() = testCreateException(::ArithmeticException, ::ArithmeticException)

    @Test fun noWhenBranchMatchedException() = testCreateException(::NoWhenBranchMatchedException, ::NoWhenBranchMatchedException, ::NoWhenBranchMatchedException, ::NoWhenBranchMatchedException)
    @Test fun uninitializedPropertyAccessException() = testCreateException(::UninitializedPropertyAccessException, ::UninitializedPropertyAccessException, ::UninitializedPropertyAccessException, ::UninitializedPropertyAccessException)

    @Test fun assertionError() = testCreateException(::AssertionError, ::AssertionError, ::AssertionError)


    private fun <T : Throwable> testCreateException(
        noarg: () -> T,
        fromMessage: (String?) -> T,
        fromCause: ((Throwable?) -> T)? = null,
        fromMessageCause: ((String?, Throwable?) -> T)? = null
    ) {
        noarg().let { e ->
            assertEquals(null, e.message)
            assertEquals(null, e.cause)
        }

        fromMessage("message").let { e ->
            assertEquals("message", e.message)
            assertEquals(null, e.cause)
        }

        fromMessage(null).let { e ->
            assertTrue(e.message == null || e.message == "null")
        }

        fromMessageCause?.run {
            invoke("message", cause).let { e ->
                assertEquals("message", e.message)
                assertSame(cause, e.cause)
            }
            invoke(null, null).let { e ->
                assertEquals(null, e.message)
                assertEquals(null, e.cause)
            }
        }

        fromCause?.invoke(cause)?.let { e ->
            assertSame(cause, e.cause)
        }
    }

}