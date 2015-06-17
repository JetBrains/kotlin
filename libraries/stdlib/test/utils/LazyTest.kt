package test.utils

import kotlin.*
import kotlin.test.*
import org.junit.Test as test

class LazyTest {

    test fun initializationCalledOnce() {
        var callCount = 0
        val lazyInt = lazy { ++callCount }

        assertEquals(0, callCount)
        assertEquals(1, lazyInt.value)
        assertEquals(1, callCount)

        lazyInt.value
        assertEquals(1, callCount)
    }

    test fun valueCreated() {
        var callCount = 0
        val lazyInt = lazy { ++callCount }

        assertFalse(lazyInt.isInitialized())
        assertEquals(0, callCount)

        lazyInt.value

        assertTrue(lazyInt.isInitialized())
    }


    test fun lazyToString() {
        var callCount = 0
        val lazyInt = lazy { ++callCount }

        assertNotEquals("1", lazyInt.toString())
        assertEquals(0, callCount)

        assertEquals(1, lazyInt.value)
        assertEquals("1", lazyInt.toString())
        assertEquals(1, callCount)
    }
}