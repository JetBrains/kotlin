package test.collections

import kotlin.*
import kotlin.io.*
import kotlin.util.*
import kotlin.test.*
import java.util.*
import java.io.*
import junit.framework.TestCase

class OldStdlibTest() : TestCase() {
    fun testCollectionEmpty() {
        assertNot {
            Arrays.asList(0, 1, 2)?.empty ?: false
        }
    }

    fun testCollectionSize() {
        assertTrue {
            Arrays.asList(0, 1, 2)?.size == 3
        }
    }

    fun testInputStreamIterator() {
        val x = ByteArray (10)

        for(index in 0..9) {
            x [index] = index.toByte()
        }

        for(b in x.inputStream) {
           System.out?.println(b)
        }
    }
}
