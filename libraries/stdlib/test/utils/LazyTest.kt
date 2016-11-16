package test.utils

import kotlin.*
import kotlin.test.*
import org.junit.Test

class LazyTest {

    @Test fun initializationCalledOnce() {
        var callCount = 0
        val lazyInt = lazy { ++callCount }

        assertEquals(0, callCount)
        assertFalse(lazyInt.isInitialized())
        assertEquals(1, lazyInt.value)
        assertEquals(1, callCount)
        assertTrue(lazyInt.isInitialized())

        lazyInt.value
        assertEquals(1, callCount)
    }

    @Test fun alreadyInitialized() {
        val lazyInt = lazyOf(1)

        assertTrue(lazyInt.isInitialized())
        assertEquals(1, lazyInt.value)
    }


    @Test fun lazyToString() {
        var callCount = 0
        val lazyInt = lazy { ++callCount }

        assertNotEquals("1", lazyInt.toString())
        assertEquals(0, callCount)

        assertEquals(1, lazyInt.value)
        assertEquals("1", lazyInt.toString())
        assertEquals(1, callCount)
    }
}