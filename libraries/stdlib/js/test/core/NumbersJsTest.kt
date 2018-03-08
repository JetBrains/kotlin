package test.js

import kotlin.test.*

class NumbersJsTest {

    @Test fun longMinMaxValues() {
        assertEquals(js("Kotlin.Long.MIN_VALUE"), Long.MIN_VALUE)
        assertEquals(js("Kotlin.Long.MAX_VALUE"), Long.MAX_VALUE)
    }

    @Test fun doubleMinMaxValues() {
        assertEquals(js("Number.MIN_VALUE"), Double.MIN_VALUE)
        assertEquals(js("Number.MAX_VALUE"), Double.MAX_VALUE)
        assertEquals(js("Number.POSITIVE_INFINITY"), Double.POSITIVE_INFINITY)
        assertEquals(js("Number.NEGATIVE_INFINITY"), Double.NEGATIVE_INFINITY)
    }

    @Test fun floatMinMaxValues() {
        assertEquals(js("Number.MIN_VALUE"), Float.MIN_VALUE)
        assertEquals(js("Number.MAX_VALUE"), Float.MAX_VALUE)
        assertEquals(js("Number.POSITIVE_INFINITY"), Float.POSITIVE_INFINITY)
        assertEquals(js("Number.NEGATIVE_INFINITY"), Float.NEGATIVE_INFINITY)
    }
}