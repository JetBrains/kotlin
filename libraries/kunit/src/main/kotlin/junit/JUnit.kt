package org.jetbrains.kotlin.test.junit

import kotlin.test.Asserter
import org.junit.Assert

class JUnitAsserter : Asserter {
    public override fun assertEquals(message : String, expected : Any?, actual : Any?) {
        Assert.assertEquals(message, expected, actual)
    }

    public override fun assertNotNull(message : String, actual : Any?) {
        Assert.assertNotNull(message, actual)
    }

    public override fun assertNull(message : String, actual : Any?) {
        Assert.assertNull(message, actual)
    }

    public override fun assertTrue(message : String, actual : Boolean) {
        Assert.assertTrue(message, actual)
    }

    public override fun fail(message : String) {
        Assert.fail(message)
    }
}