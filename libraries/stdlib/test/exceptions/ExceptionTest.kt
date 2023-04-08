/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.exceptions

import kotlin.test.*

@Suppress("Reformat") // author's formatting
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
    @Test fun concurrentModificationException() = testCreateException(::ConcurrentModificationException, ::ConcurrentModificationException, ::ConcurrentModificationException, ::ConcurrentModificationException)
    @Test fun arithmeticException() = testCreateException(::ArithmeticException, ::ArithmeticException)

    @Test fun noWhenBranchMatchedException() = @Suppress("DEPRECATION_ERROR") testCreateException(::NoWhenBranchMatchedException, ::NoWhenBranchMatchedException, ::NoWhenBranchMatchedException, ::NoWhenBranchMatchedException)
    @Test fun uninitializedPropertyAccessException() = @Suppress("DEPRECATION_ERROR") testCreateException(::UninitializedPropertyAccessException, ::UninitializedPropertyAccessException, ::UninitializedPropertyAccessException, ::UninitializedPropertyAccessException)

    @Test fun assertionError() = testCreateException(::AssertionError, ::AssertionError, ::AssertionError, ::AssertionError)


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

    @Test
    fun suppressedExceptions() {
        val e1 = Throwable()

        val c1 = Exception("Suppressed 1")
        val c2 = Exception("Suppressed 2")

        assertTrue(e1.suppressedExceptions.isEmpty())

        e1.addSuppressed(c1)
        e1.addSuppressed(c2)

        assertEquals(listOf(c1, c2), e1.suppressedExceptions)
    }

    @Test
    fun exceptionDetailedTrace() {
        fun root(): Nothing = throw IllegalStateException("Root cause\nDetails: root")

        fun suppressedError(id: Int): Throwable = UnsupportedOperationException("Side error\nId: $id")

        fun induced(): Nothing {
            try {
                root()
            } catch (e: Throwable) {
                for (id in 0..1)
                    e.addSuppressed(suppressedError(id))
                throw RuntimeException("Induced", e)
            }
        }

        val e = try {
            induced()
        } catch (e: Throwable) {
            e.apply { addSuppressed(suppressedError(2)) }
        }

        val topLevelTrace = e.stackTraceToString()
        fun assertInTrace(value: Any) {
            if (value.toString() !in topLevelTrace) {
                fail("Expected top level trace: $topLevelTrace\n\nto contain: $value")
            }
        }

        assertInTrace(e)

        val cause = assertNotNull(e.cause, "Should have cause")
        assertInTrace(cause)

        val topLevelSuppressed = e.suppressedExceptions.single()
        assertInTrace(topLevelSuppressed)
        cause.suppressedExceptions.forEach {
            assertInTrace(it)
        }

//        fail(topLevelTrace) // to dump the entire trace
    }

    @Test
    fun circularSuppressedDetailedTrace() {
        // Testing an exception of the following structure
        // e1
        //    -- suppressed: e0 (same stack as e1)
        //    -- suppressed: e3
        //       -- suppressed: e1
        // Caused by: e2
        //    -- suppressed: e1
        // Caused by: e3

        val e3 = Exception("e3")
        val e2 = Error("e2", e3)
        val (e1, e0) = listOf("e1", "e0").map { msg -> RuntimeException(msg, e2.takeIf { msg == "e1" }) }
        e1.addSuppressed(e0)
        e1.addSuppressed(e3)
        e3.addSuppressed(e1)
        e2.addSuppressed(e1)

        val topLevelTrace = e1.stackTraceToString()
        fun assertAppearsInTrace(value: Any, count: Int) {
            if (Regex.fromLiteral(value.toString()).findAll(topLevelTrace).count() != count) {
                fail("Expected to find $value $count times in $topLevelTrace")
            }
        }
        assertAppearsInTrace(e1, 3)
        assertAppearsInTrace(e0, 1)
        assertAppearsInTrace(e2, 1)
        assertAppearsInTrace(e3, 2)
//        fail(topLevelTrace) // to dump the entire trace
    }

}