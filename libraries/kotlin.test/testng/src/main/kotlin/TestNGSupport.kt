/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test.testng

import org.testng.*
import kotlin.test.*

class TestNGContributor : AsserterContributor {
    override fun contribute(): Asserter? {
        for (stackFrame in currentStackTrace()) {
            val className = stackFrame.className

            if (className.startsWith("org.testng.") || className.startsWith("testng.")) {
                return TestNGAsserter
            }
        }

        return null
    }
}

object TestNGAsserter : Asserter {
    override fun assertEquals(message: String?, expected: Any?, actual: Any?) {
        Assert.assertEquals(expected, actual, message)
    }

    override fun assertNotEquals(message: String?, illegal: Any?, actual: Any?) {
        Assert.assertNotEquals(illegal, actual, message)
    }

    override fun assertSame(message: String?, expected: Any?, actual: Any?) {
        Assert.assertSame(expected, actual, message)
    }

    override fun assertNotSame(message: String?, illegal: Any?, actual: Any?) {
        Assert.assertNotSame(illegal, actual, message)
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
}
