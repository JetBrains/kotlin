package kotlin.test.junit

import org.junit.*
import kotlin.test.*

class JUnitContributor : AsserterContributor {
    override fun contribute(): Asserter? {
        for (stackFrame in currentStackTrace()) {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            val className = stackFrame.className as java.lang.String

            if (className.startsWith("org.junit.") || className.startsWith("junit.")) {
                return JUnitAsserter
            }
        }

        return null
    }
}

object JUnitAsserter : Asserter {
    override fun assertEquals(message : String?, expected : Any?, actual : Any?) {
        Assert.assertEquals(message, expected, actual)
    }

    override fun assertNotEquals(message : String?, illegal : Any?, actual : Any?) {
        Assert.assertNotEquals(message, illegal, actual)
    }

    override fun assertNotNull(message : String?, actual : Any?) {
        Assert.assertNotNull(message ?: "actual value is null", actual)
    }

    override fun assertNull(message : String?, actual : Any?) {
        Assert.assertNull(message ?: "actual value is not null", actual)
    }

    override fun fail(message : String?): Nothing {
        Assert.fail(message)
        // should not get here
        throw AssertionError(message)
    }
}
