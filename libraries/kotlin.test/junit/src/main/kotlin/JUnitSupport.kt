/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.junit

import org.junit.*
import kotlin.test.*

/**
 * Provides [JUnitAsserter] if `org.junit.Assert` is found in the classpath.
 */
public class JUnitContributor : AsserterContributor {
    override fun contribute(): Asserter? {
        return if (hasJUnitInClassPath) JUnitAsserter else null
    }

    private val hasJUnitInClassPath = try {
        Class.forName("org.junit.Assert")
        true
    } catch (_: ClassNotFoundException) {
        false
    }
}

/**
 * Implements `kotlin.test` assertions by delegating them to `org.junit.Assert` class.
 */
public object JUnitAsserter : Asserter {
    override fun assertEquals(message: String?, expected: Any?, actual: Any?) {
        Assert.assertEquals(message, expected, actual)
    }

    override fun assertNotEquals(message: String?, illegal: Any?, actual: Any?) {
        Assert.assertNotEquals(message, illegal, actual)
    }

    override fun assertSame(message: String?, expected: Any?, actual: Any?) {
        Assert.assertSame(message, expected, actual)
    }

    override fun assertNotSame(message: String?, illegal: Any?, actual: Any?) {
        Assert.assertNotSame(message, illegal, actual)
    }

    override fun assertNotNull(message: String?, actual: Any?) {
        Assert.assertNotNull(message ?: "actual value is null", actual)
    }

    override fun assertNull(message: String?, actual: Any?) {
        Assert.assertNull(message ?: "actual value is not null", actual)
    }

    override fun fail(message: String?): Nothing {
        Assert.fail(message)
        // should not get here
        throw AssertionError(message)
    }

    @SinceKotlin("1.4")
    override fun fail(message: String?, cause: Throwable?): Nothing {
        try {
            Assert.fail(message)
        } catch (e: AssertionError) {
            e.initCause(cause)
            throw e
        }
        // should not get here
        throw AssertionError(message).initCause(cause)
    }
}
