package org.jetbrains.kotlin.gradle

import org.junit.Assert
import org.junit.Test

class InternalDummyTest {
    @Test
    fun testInternalDummy() {
        val dummy = InternalDummy("World")
        Assert.assertEquals("Hello World!", dummy.greeting)
    }
}