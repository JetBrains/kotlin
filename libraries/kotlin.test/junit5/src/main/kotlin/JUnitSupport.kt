/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.junit5

import org.junit.jupiter.api.Assertions
import kotlin.test.*

/**
 * Provides [JUnit5Asserter] if `org.junit.jupiter.api.Assertions` class is found in the classpath.
 */
public class JUnit5Contributor : AsserterContributor {
    override fun contribute(): Asserter? {
        return if (hasJUnitInClassPath) JUnit5Asserter else null
    }

    private val hasJUnitInClassPath = try {
        Class.forName("org.junit.jupiter.api.Assertions")
        true
    } catch (_: ClassNotFoundException) {
        false
    }
}

/**
 * Implements `kotlin.test` assertions by delegating them to `org.junit.jupiter.api.Assertions` class.
 */
public object JUnit5Asserter : Asserter {
    override fun assertEquals(message: String?, expected: Any?, actual: Any?) {
        Assertions.assertEquals(expected, actual, message)
    }

    override fun assertNotEquals(message: String?, illegal: Any?, actual: Any?) {
        Assertions.assertNotEquals(illegal, actual, message)
    }

    override fun assertSame(message: String?, expected: Any?, actual: Any?) {
        Assertions.assertSame(expected, actual, message)
    }

    override fun assertNotSame(message: String?, illegal: Any?, actual: Any?) {
        Assertions.assertNotSame(illegal, actual, message)
    }

    override fun assertNotNull(message: String?, actual: Any?) {
        Assertions.assertNotNull(actual, message ?: "actual value is null")
    }

    override fun assertNull(message: String?, actual: Any?) {
        Assertions.assertNull(actual, message ?: "actual value is not null")
    }

    override fun fail(message: String?): Nothing {
        Assertions.fail<Any?>(message)
        // should not get here
        throw AssertionError(message)
    }

    @SinceKotlin("1.4")
    override fun fail(message: String?, cause: Throwable?): Nothing {
        Assertions.fail<Any?>(message, cause)
        // should not get here
        throw AssertionError(message, cause)
    }
}
