/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.testng

import org.testng.*
import kotlin.test.*

/**
 * Provides [TestNGAsserter] if `org.testng.Assert` is found in the classpath.
 */
public class TestNGContributor : AsserterContributor {
    override fun contribute(): Asserter? {
        return if (hasTestNGInClassPath) TestNGAsserter else null
    }

    private val hasTestNGInClassPath = try {
        Class.forName("org.testng.Assert")
        true
    } catch (_: ClassNotFoundException) {
        false
    }
}

/**
 * Implements `kotlin.test` assertions by delegating them to `org.testng.Assert` class.
 */
public object TestNGAsserter : Asserter {
    override fun assertEquals(message: String?, expected: Any?, actual: Any?) {
        Assert.assertEquals(actual, expected, message)
    }

    override fun assertNotEquals(message: String?, illegal: Any?, actual: Any?) {
        Assert.assertNotEquals(actual, illegal, message)
    }

    override fun assertSame(message: String?, expected: Any?, actual: Any?) {
        Assert.assertSame(actual, expected, message)
    }

    override fun assertNotSame(message: String?, illegal: Any?, actual: Any?) {
        Assert.assertNotSame(actual, illegal, message)
    }

    override fun assertNotNull(message: String?, actual: Any?) {
        Assert.assertNotNull(actual, message ?: "actual value is null")
    }

    override fun assertNull(message: String?, actual: Any?) {
        Assert.assertNull(actual, message ?: "actual value is not null")
    }

    override fun fail(message: String?): Nothing {
        Assert.fail(message)
        // should not get here
        throw AssertionError(message)
    }

    @SinceKotlin("1.4")
    override fun fail(message: String?, cause: Throwable?): Nothing {
        Assert.fail(message, cause)
        // should not get here
        throw AssertionError(message).initCause(cause)
    }
}
