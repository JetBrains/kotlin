package org.jetbrains.kotlin.test.junit

import kotlin.test.Asserter
import org.junit.Assert

class JUnitAsserter : Asserter {
    override fun assertEquals(message : String, expected : Any?, actual : Any?) {
        Assert.assertEquals(message, expected, actual)
    }

    override fun assertNotEquals(message : String, illegal : Any?, actual : Any?) {
        Assert.assertNotEquals(message, illegal, actual)
    }

    override fun assertNotNull(message : String, actual : Any?) {
        Assert.assertNotNull(message, actual)
    }

    override fun assertNull(message : String, actual : Any?) {
        Assert.assertNull(message, actual)
    }

    override fun assertTrue(message : String, actual : Boolean) {
        Assert.assertTrue(message, actual)
    }

    override fun fail(message : String) {
        Assert.fail(message)
    }
}